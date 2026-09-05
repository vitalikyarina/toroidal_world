package com.toroidalworld.compat.sable;

import org.jspecify.annotations.Nullable;

public interface SableMotorGoalHolder {
    @Nullable SableMotorGoal toroidal$motorGoal();

    void toroidal$motorGoal(@Nullable SableMotorGoal goal);
}
