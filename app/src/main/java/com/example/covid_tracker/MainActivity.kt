package com.example.covid_tracker

import android.content.Context
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.internal.UnsafeAllocator.create
import com.robinhood.spark.SparkAdapter
import com.robinhood.ticker.TickerUtils
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.gson.GsonConverterFactory.create
import java.net.URI.create
import java.sql.Time
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.DoubleStream.builder

private const val  BASE_URL="https://api.covidtracking.com/v1/"
private const val TAG="MainActivity"
private const val ALL_STATES=" All (Nationwide)"

class MainActivity : AppCompatActivity() {

    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var  adapter:CovidSparkAdapter
    private lateinit var  nationaldailydata:List<CovidData>
    private lateinit var perStateDailyData:Map<String,List<CovidData>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Fetch the national data

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val covidservice = retrofit.create(CovidService::class.java)
        covidservice.getnationaldata().enqueue(object : Callback<List<CovidData>> {
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.d(TAG, "On Failure $t")
            }

            override fun onResponse(call: Call<List<CovidData>>, response: Response<List<CovidData>>)
            {
                Log.i(TAG, "On Response ${response.toString()}")
                val nationaldata = response.body()
                if (nationaldata == null) {
                    Log.d("TAG", "did not recieved any response")
                    return
                }
                    setUpEventListener()//set up event  listener
                    nationaldailydata = nationaldata.reversed()
                Log.i(TAG, "Update the graph with national data")
                updateDisplayWithData(nationaldailydata)
            }

        })


        //Fetch the state data
        covidservice.getStateData().enqueue(object : Callback<List<CovidData>> {
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.d(TAG, "On Failure $t")
            }

            override fun onResponse(call: Call<List<CovidData>>, response: Response<List<CovidData>>) {
                Log.i(TAG, "On Response ${response.toString()}")
                val statedata = response.body()
                if (statedata == null) {
                    Log.d("TAG", "did not recieved any response")
                    return
                }
                else
                    perStateDailyData = statedata.reversed().groupBy { it.state }
                //by this the data is chronologically increasing
                Log.i(TAG, "Update the spinner with state names")

                //updating the spinner with state names
                updateSpinnerWithStateData(perStateDailyData.keys)

            }

        })
    }

    private fun updateSpinnerWithStateData(statenames: Set<String>)
    {
        val stateAbbreviationList=statenames.toMutableList()
        stateAbbreviationList.sort()// sorting on the basis of the starting letter of the word
        stateAbbreviationList.add(0,ALL_STATES)
        //Add the state list as a data source to our spinner
        spinnerSelect.attachDataSource(stateAbbreviationList)
        Log.i("Spinner", spinnerSelect.getItemAtPosition(1) as String)
        spinnerSelect.setOnSpinnerItemSelectedListener { parent,_, position,_ ->
            val selectedState=parent.getItemAtPosition(position) as String
            val selectedData=perStateDailyData[selectedState]?:nationaldailydata
            updateDisplayWithData(selectedData)
        }
    }

    private fun setUpEventListener() {
        tickerView.setCharacterLists(TickerUtils.provideNumberList())
        //want to make the UI interactive only when there is data to show
        //Add a listener
        sparView.isScrubEnabled=true
        sparView.setScrubListener {itemData->
          if(itemData is CovidData)
          {
              updateInfoForDate(itemData)
          }
        }
       //respond to the radio button selected events
        radioGroupTimeSelection.setOnCheckedChangeListener { radioGroup, checkedId ->
            adapter.daysAgo=when(checkedId)
            {
                R.id.radioButtonWeek->TimeScale.WEEK
                R.id.radioButtonMonth->TimeScale.MONTH
                else->TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
            //notify the adpater that the undermine data has changed
            //checked id is the id of the radio button selected
        }

        radioGroupMatrixSelection.setOnCheckedChangeListener { radioGroup, checkedId ->

            when(checkedId)
            {
                R.id.radioButtonPositive->updateDisplayMetric(Metric.POSITIVE)
                R.id.radioButtonNegative->updateDisplayMetric(Metric.NEGATIVE)
                R.id.radioButtonDeath->updateDisplayMetric(Metric.DEATH)
            }
        }

    }

    private fun updateDisplayMetric(metric: Metric) {
        //update the color of the chart

        val colorRes=when(metric)
        {
            Metric.NEGATIVE->R.color.colorNegative
            Metric.DEATH->R.color.colorDeath
            Metric.POSITIVE->R.color.colorPositive
        }

      val colorInt=ContextCompat.getColor(this,colorRes)
        sparView.lineColor=colorInt
       tickerView.setTextColor(colorRes)
      adapter.metric=metric
        adapter.notifyDataSetChanged()
        updateInfoForDate(currentlyShownData.last())
        //we have done currentlyShownData.last() because we want to get the recent data
    }

    private fun updateDisplayWithData(dailydata:List<CovidData>)
    {
        currentlyShownData=dailydata
        //create a new spark adapter
        adapter=CovidSparkAdapter(dailydata)
        sparView.adapter=adapter

        //update the radio buttons to select the positive cases and max time by default
        //display metric for the most recent date

        radioButtonMax.isChecked=true
        radioButtonPositive.isChecked=true

        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData:CovidData) {

        val numCases=when(adapter.metric)
        {
            Metric.NEGATIVE->covidData.negativeIncrease
            Metric.DEATH->covidData.deathIncrease
            Metric.POSITIVE->covidData.positiveIncrease
        }
       tickerView.text= NumberFormat.getInstance().format(numCases)
        val outputDateFormat=SimpleDateFormat("MMM d,yyyy", Locale.US)
        tvDateLabel.text=outputDateFormat.format(covidData.dateChecked)

    }
}