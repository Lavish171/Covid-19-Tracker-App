package com.example.covid_tracker

enum class Metric
{
    NEGATIVE,POSITIVE,DEATH
}

enum class TimeScale(val numDays:Int)
{
    WEEK(7),
    MONTH(30),
    MAX(-1)
    //as we do not know that how many days are associated with the maximum
}

