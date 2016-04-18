package jacz.util.io.serialization.localstorage;

import jacz.storage.ActiveJDBCController;
import jacz.util.objects.Util;
import org.javalite.activejdbc.DB;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A local storage implementation backed by SQLite 3 databases. Data access is performed via the ActiveJDBC orm
 * <p/>
 * A write-through cache is maintained for all written data, so accessions do not go to the database.
 */
public class LocalStorage {

    private static class TableField {

        final String name;

        final String type;

        public TableField(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    static final String DATABASE = "localStorage";

    private static final String METADATA_TABLE = "metadata";

    private static final String ITEMS_TABLE = "items";

    private static final TableField ID = new TableField("id", "INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT");

    private static final TableField LS_VERSION = new TableField("ls_version", "TEXT");

    private static final TableField CREATION_DATE = new TableField("creation_date", "INTEGER");

    private static final TableField NAME = new TableField("id", "TEXT NOT NULL PRIMARY KEY");

    private static final TableField STRING_ITEM = new TableField("string_item", "TEXT");

    private static final TableField INTEGER_ITEM = new TableField("integer_item", "INTEGER");

    private static final TableField REAL_ITEM = new TableField("real_item", "REAL");

    private static final String LIST_SEPARATOR = "\n";


    public static final String CURRENT_VERSION = "0.1.0";


    /**
     * Path to the local database
     */
    private final String path;

    private final Map<String, String> stringItems;

    private final Map<String, Boolean> booleanItems;

    private final Map<String, Byte> byteItems;

    private final Map<String, Short> shortItems;

    private final Map<String, Integer> integerItems;

    private final Map<String, Long> longItems;

    private final Map<String, Float> floatItems;

    private final Map<String, Double> doubleItems;

    private final Map<String, Date> dateItems;

    public LocalStorage(String path) throws IOException {
        this.path = path;
        stringItems = Collections.synchronizedMap(new HashMap<String, String>());
        booleanItems = Collections.synchronizedMap(new HashMap<String, Boolean>());
        byteItems = Collections.synchronizedMap(new HashMap<String, Byte>());
        shortItems = Collections.synchronizedMap(new HashMap<String, Short>());
        integerItems = Collections.synchronizedMap(new HashMap<String, Integer>());
        longItems = Collections.synchronizedMap(new HashMap<String, Long>());
        floatItems = Collections.synchronizedMap(new HashMap<String, Float>());
        doubleItems = Collections.synchronizedMap(new HashMap<String, Double>());
        dateItems = Collections.synchronizedMap(new HashMap<String, Date>());
    }

    public static LocalStorage createNew(String path) throws IOException {
        DB db = ActiveJDBCController.connect(DATABASE, path);
        db.exec("DROP TABLE IF EXISTS " + METADATA_TABLE);
        db.exec("DROP TABLE IF EXISTS " + ITEMS_TABLE);

        StringBuilder create = new StringBuilder("CREATE TABLE ").append(METADATA_TABLE).append("(");
        appendField(create, ID, false);
        appendField(create, LS_VERSION, false);
        appendField(create, CREATION_DATE, true);
        db.exec(create.toString());

        create = new StringBuilder("CREATE TABLE ").append(ITEMS_TABLE).append("(");
        appendField(create, NAME, false);
        appendField(create, STRING_ITEM, false);
        appendField(create, INTEGER_ITEM, false);
        appendField(create, REAL_ITEM, true);
        db.exec(create.toString());

        Metadata Metadata = new Metadata();
        Metadata.setString(LS_VERSION.name, CURRENT_VERSION);
        Metadata.setLong(CREATION_DATE.name, new Date().getTime());
        Metadata.saveIt();

        ActiveJDBCController.disconnect();
        return new LocalStorage(path);
    }

    private static void appendField(StringBuilder create, TableField field, boolean isFinal) {
        create.append(field.name).append(" ").append(field.type);
        if (isFinal) {
            create.append(")");
        } else {
            create.append(",");
        }
    }

    private Metadata getMetadata() {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            return (Metadata) Metadata.findAll().get(0);
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    private Item getItem(String name, boolean create) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = Item.findFirst(NAME.name + " = ?", name);
            if (item == null && create) {
                item = new Item();
                item.setString(NAME.name, name);
                item.insert();
            }
            return item;
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public String getLocalStorageVersion() {
        return getMetadata().getString(LS_VERSION.name);
    }

    public Date getCreationDate() {
        Long date = getMetadata().getLong(CREATION_DATE.name);
        return date != null ? new Date(date) : null;
    }

    public int itemCount() {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            return Item.count().intValue();
        } finally {
            ActiveJDBCController.disconnect();
        }

    }

    public boolean containsItem(String name) {
        return getItem(name, false) != null;
    }

    public void removeItem(String name) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = getItem(name, false);
            if (item != null) {
                item.delete();
            }
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    private <E> E loadCache(Map<String, E> cache, String name, E value) {
        cache.put(name, value);
        return value;
    }

    public String getString(String name) {
        if (stringItems.containsKey(name)) {
            return stringItems.get(name);
        } else {
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, false);
                return loadCache(stringItems, name, item != null ? item.getString(STRING_ITEM.name) : null);
            } finally {
                ActiveJDBCController.disconnect();
            }
        }
    }

    public boolean setString(String name, String value) {
        String storedValue = getString(name);
        if (value == null || !Util.equals(value, storedValue)) {
            loadCache(stringItems, name, value);
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, true);
                item.setString(STRING_ITEM.name, value);
                saveItem(item);
                return true;
            } finally {
                ActiveJDBCController.disconnect();
            }
        } else {
            return false;
        }
    }

    public Boolean getBoolean(String name) {
        if (booleanItems.containsKey(name)) {
            return booleanItems.get(name);
        } else {
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, false);
                return loadCache(booleanItems, name, item != null ? item.getBoolean(INTEGER_ITEM.name) : null);
            } finally {
                ActiveJDBCController.disconnect();
            }
        }
    }

    public boolean setBoolean(String name, Boolean value) {
        Boolean storedValue = getBoolean(name);
        if (value == null || !Util.equals(value, storedValue)) {
            loadCache(booleanItems, name, value);
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, true);
                item.setBoolean(INTEGER_ITEM.name, value);
                saveItem(item);
                return true;
            } finally {
                ActiveJDBCController.disconnect();
            }
        } else {
            return false;
        }
    }

    public Byte getByte(String name) {
        if (byteItems.containsKey(name)) {
            return byteItems.get(name);
        } else {
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, false);
                return loadCache(byteItems, name, item != null ? item.getInteger(INTEGER_ITEM.name).byteValue() : null);
            } finally {
                ActiveJDBCController.disconnect();
            }
        }
    }

    public boolean setByte(String name, Byte value) {
        Byte storedValue = getByte(name);
        if (value == null || !Util.equals(value, storedValue)) {
            loadCache(byteItems, name, value);
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, true);
                item.setInteger(INTEGER_ITEM.name, value);
                saveItem(item);
                return true;
            } finally {
                ActiveJDBCController.disconnect();
            }
        } else {
            return false;
        }
    }

    public Short getShort(String name) {
        if (shortItems.containsKey(name)) {
            return shortItems.get(name);
        } else {
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, false);
                return loadCache(shortItems, name, item != null ? item.getShort(INTEGER_ITEM.name) : null);
            } finally {
                ActiveJDBCController.disconnect();
            }
        }
    }

    public boolean setShort(String name, Short value) {
        Short storedValue = getShort(name);
        if (value == null || !Util.equals(value, storedValue)) {
            loadCache(shortItems, name, value);
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, true);
                item.setShort(INTEGER_ITEM.name, value);
                saveItem(item);
                return true;
            } finally {
                ActiveJDBCController.disconnect();
            }
        } else {
            return false;
        }
    }

    public Integer getInteger(String name) {
        if (integerItems.containsKey(name)) {
            return integerItems.get(name);
        } else {
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, false);
                return loadCache(integerItems, name, item != null ? item.getInteger(INTEGER_ITEM.name) : null);
            } finally {
                ActiveJDBCController.disconnect();
            }
        }
    }

    public boolean setInteger(String name, Integer value) {
        Integer storedValue = getInteger(name);
        if (value == null || !Util.equals(value, storedValue)) {
            loadCache(integerItems, name, value);
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, true);
                item.setInteger(INTEGER_ITEM.name, value);
                saveItem(item);
                return true;
            } finally {
                ActiveJDBCController.disconnect();
            }
        } else {
            return false;
        }
    }

    public Long getLong(String name) {
        if (longItems.containsKey(name)) {
            return longItems.get(name);
        } else {
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, false);
                return loadCache(longItems, name, item != null ? item.getLong(INTEGER_ITEM.name) : null);
            } finally {
                ActiveJDBCController.disconnect();
            }
        }
    }

    public boolean setLong(String name, Long value) {
        Long storedValue = getLong(name);
        if (value == null || !Util.equals(value, storedValue)) {
            loadCache(longItems, name, value);
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, true);
                item.setLong(INTEGER_ITEM.name, value);
                saveItem(item);
                return true;
            } finally {
                ActiveJDBCController.disconnect();
            }
        } else {
            return false;
        }
    }

    public Float getFloat(String name) {
        if (floatItems.containsKey(name)) {
            return floatItems.get(name);
        } else {
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, false);
                return loadCache(floatItems, name, item != null ? item.getFloat(REAL_ITEM.name) : null);
            } finally {
                ActiveJDBCController.disconnect();
            }
        }
    }

    public boolean setFloat(String name, Float value) {
        Float storedValue = getFloat(name);
        if (value == null || !Util.equals(value, storedValue)) {
            loadCache(floatItems, name, value);
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, true);
                item.setFloat(REAL_ITEM.name, value);
                saveItem(item);
                return true;
            } finally {
                ActiveJDBCController.disconnect();
            }
        } else {
            return false;
        }
    }

    public Double getDouble(String name) {
        if (doubleItems.containsKey(name)) {
            return doubleItems.get(name);
        } else {
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, false);
                return loadCache(doubleItems, name, item != null ? item.getDouble(REAL_ITEM.name) : null);
            } finally {
                ActiveJDBCController.disconnect();
            }
        }
    }

    public boolean setDouble(String name, Double value) {
        Double storedValue = getDouble(name);
        if (value == null || !Util.equals(value, storedValue)) {
            loadCache(doubleItems, name, value);
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, true);
                item.setDouble(REAL_ITEM.name, value);
                saveItem(item);
                return true;
            } finally {
                ActiveJDBCController.disconnect();
            }
        } else {
            return false;
        }
    }

    public Date getDate(String name) {
        if (dateItems.containsKey(name)) {
            return dateItems.get(name);
        } else {
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, false);
                if (item != null) {
                    Long date = item.getLong(INTEGER_ITEM.name);
                    return loadCache(dateItems, name, date != null ? new Date(date) : null);
                } else {
                    return null;
                }
            } finally {
                ActiveJDBCController.disconnect();
            }
        }
    }

    public boolean setDate(String name, Date value) {
        Date storedValue = getDate(name);
        if (value == null || !Util.equals(value, storedValue)) {
            loadCache(dateItems, name, value);
            ActiveJDBCController.connect(DATABASE, path);
            try {
                Item item = getItem(name, true);
                item.setLong(INTEGER_ITEM.name, value);
                saveItem(item);
                return true;
            } finally {
                ActiveJDBCController.disconnect();
            }
        } else {
            return false;
        }
    }

    public <E> E getEnum(String name, Class<E> enum_) {
        try {
            String str = getString(name);
            if (str != null) {
                Method valueOf = enum_.getMethod("valueOf", String.class);
                return (E) valueOf.invoke(null, str);
            } else {
                return null;
            }
        } catch (Exception e) {
            // cannot happen
            // todo fatal error
            return null;
        }
    }

    public <E> boolean setEnum(String name, Class<E> enum_, E value) {
        try {
            Method getName = enum_.getMethod("name");
            return setString(name, (String) getName.invoke(value));
        } catch (Exception e) {
            // cannot happen
            // todo fatal error
            return false;
        }
    }

    public List<String> getStringList(String name) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = getItem(name, false);
            return item != null ? deserializeList(item.getString(STRING_ITEM.name)) : null;
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public void setStringList(String name, List<String> list) {
        setList(name, list);
    }

    public List<Boolean> getBooleanList(String name) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = getItem(name, false);
            if (item != null) {
                List<Boolean> values = new ArrayList<>();
                for (String str : getStringList(name)) {
                    values.add(Boolean.parseBoolean(str));
                }
                return values;
            } else {
                return null;
            }
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public void setBooleanList(String name, List<Boolean> list) {
        setList(name, list);
    }

    public List<Byte> getByteList(String name) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = getItem(name, false);
            if (item != null) {
                List<Byte> values = new ArrayList<>();
                for (String str : getStringList(name)) {
                    values.add(Byte.parseByte(str));
                }
                return values;
            } else {
                return null;
            }
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public void setByteList(String name, List<Byte> list) {
        setList(name, list);
    }

    public List<Short> getShortList(String name) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = getItem(name, false);
            if (item != null) {
                List<Short> values = new ArrayList<>();
                for (String str : getStringList(name)) {
                    values.add(Short.parseShort(str));
                }
                return values;
            } else {
                return null;
            }
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public void setShortList(String name, List<Short> list) {
        setList(name, list);
    }

    public List<Integer> getIntegerList(String name) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = getItem(name, false);
            if (item != null) {
                List<Integer> values = new ArrayList<>();
                for (String str : getStringList(name)) {
                    values.add(Integer.parseInt(str));
                }
                return values;
            } else {
                return null;
            }
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public void setIntegerList(String name, List<Integer> list) {
        setList(name, list);
    }

    public List<Long> getLongList(String name) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = getItem(name, false);
            if (item != null) {
                List<Long> values = new ArrayList<>();
                for (String str : getStringList(name)) {
                    values.add(Long.parseLong(str));
                }
                return values;
            } else {
                return null;
            }
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public void setLongList(String name, List<Long> list) {
        setList(name, list);
    }

    public List<Float> getFloatList(String name) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = getItem(name, false);
            if (item != null) {
                List<Float> values = new ArrayList<>();
                for (String str : getStringList(name)) {
                    values.add(Float.parseFloat(str));
                }
                return values;
            } else {
                return null;
            }
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public void setFloatList(String name, List<Float> list) {
        setList(name, list);
    }

    public List<Double> getDoubleList(String name) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = getItem(name, false);
            if (item != null) {
                List<Double> values = new ArrayList<>();
                for (String str : getStringList(name)) {
                    values.add(Double.parseDouble(str));
                }
                return values;
            } else {
                return null;
            }
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public void setDoubleList(String name, List<Double> list) {
        setList(name, list);
    }

    public List<Date> getDateList(String name) {
        List<Long> longList = getLongList(name);
        if (longList != null) {
            List<Date> list = new ArrayList<>();
            for (long value : longList) {
                list.add(new Date(value));
            }
            return list;
        } else {
            return null;
        }
    }

    public void setDateList(String name, List<Date> list) {
        List<Long> longList = new ArrayList<>();
        for (Date date : list) {
            longList.add(date.getTime());
        }
        setLongList(name, longList);
    }

    public <E> List<E> getEnumList(String name, Class<E> enum_) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = getItem(name, false);
            if (item != null) {
                Method valueOf = enum_.getMethod("valueOf", String.class);
                List<E> enumValues = new ArrayList<>();
                for (String str : getStringList(name)) {
                    enumValues.add((E) valueOf.invoke(null, str));
                }
                return enumValues;
            } else {
                return null;
            }
        } catch (Exception e) {
            // cannot happen
            // todo fatal error
            return null;
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public <E> void setEnumList(String name, Class<E> enum_, List<E> list) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = getItem(name, true);
            Method getName = enum_.getMethod("name");
            List<String> strList = new ArrayList<>();
            for (E value : list) {
                strList.add((String) getName.invoke(value));
            }
            setString(name, serializeList(strList));
            saveItem(item);
        } catch (Exception e) {
            // cannot happen
            // todo fatal error
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    private void saveItem(Item item) {
        item.saveIt();
    }

    private void setList(String name, List<?> list) {
        ActiveJDBCController.connect(DATABASE, path);
        try {
            Item item = getItem(name, true);
            item.setString(STRING_ITEM.name, serializeList(list));
            saveItem(item);
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    private String serializeList(List<?> list) {
        if (list.isEmpty()) {
            return "";
        } else {
            StringBuilder serList = new StringBuilder(LIST_SEPARATOR);
            for (Object item : list) {
                serList.append(item.toString()).append(LIST_SEPARATOR);
            }
            return serList.toString();
        }
    }

    private List<String> deserializeList(String value) {
        value = value == null ? "" : value;
        StringTokenizer tokenizer = new StringTokenizer(value, LIST_SEPARATOR);
        List<String> list = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
        return list;
    }
}
