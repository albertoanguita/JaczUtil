package jacz.util.concurrency.concurrency_controller;

import jacz.util.maps.ObjectCount;

/**
 * Actions requested by the concurrency controller
 */
public interface ConcurrencyControllerAction {

    int maxNumberOfExecutionsAllowed();

    int getActivityPriority(String activity);

    boolean activityCanExecute(String activity, ObjectCount<String> numberOfExecutionsOfActivities);

    void activityIsGoingToBegin(String activity, ObjectCount<String> numberOfExecutionsOfActivities);

    void activityHasEnded(String activity, ObjectCount<String> numberOfExecutionsOfActivities);
}
