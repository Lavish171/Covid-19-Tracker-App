package com.example.covid_tracker

import android.graphics.RectF
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val dailydata: List<CovidData>): SparkAdapter() {

    var metric=Metric.POSITIVE
    var daysAgo=TimeScale.MAX
    override fun getY(index: Int): Float {
        val chosenDayData=dailydata[index]
        return when(metric)
        {
            Metric.POSITIVE->chosenDayData.positiveIncrease.toFloat()
            Metric.NEGATIVE->chosenDayData.negativeIncrease.toFloat()
            Metric.DEATH->chosenDayData.deathIncrease.toFloat()
        }


    }

    override fun getItem(index: Int): Any {
     return dailydata[index]
    }

    override fun getCount(): Int {
       return dailydata.size
    }

    override fun getDataBounds(): RectF {
        val bounds= super.getDataBounds()
        //bounds.left= (count-7).toFloat()
        //count is containing the result of getCount() function
        if(daysAgo!=TimeScale.MAX)
        bounds.left=count-daysAgo.numDays.toFloat()
        return bounds
    }


}
