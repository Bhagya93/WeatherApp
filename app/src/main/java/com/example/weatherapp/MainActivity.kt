package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.utils.isInternetAvailable
import com.example.weatherapp.viewmodel.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import java.util.*


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    var binding: ActivityMainBinding? = null
    lateinit var mainViewModel: MainViewModel
    private var lastSearchCity = ""
    private val locationPermissionCode = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding?.root)

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        val sh = getSharedPreferences(Constants.SEARCH_CITY, MODE_PRIVATE)
        lastSearchCity = sh.getString(Constants.SEARCH_CITY_NAME, "") ?: ""

        if (isInternetAvailable(this)) {
            if (checkPermission()) {
                requestPermission()
            } else {

                if (lastSearchCity != null && lastSearchCity != "")
                    mainViewModel.getWeatherData(lastSearchCity)
                else
                    getCurrentCity()
            }
        } else {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Info")
            builder.setMessage("Internet not available, Cross check your internet connectivity and try again")
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setPositiveButton("OK") { _, _ ->
                finish()
            }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.setCancelable(false)
            alertDialog.show()
        }

        mainViewModel.weatherLiveData.observe(this, Observer { weather ->
            if (weather != null) {
                if (weather.sys?.country == Constants.COUNTRY_CODE) {
                    binding?.weatherView?.visibility = View.VISIBLE
                    binding?.textViewCityName?.text = weather.name
                    binding?.textViewCityWeather?.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    val str: String? = weather.weather.get(0).description
                    val sb = StringBuilder(str)
                    sb.setCharAt(0, Character.toUpperCase(sb[0]))
                    binding?.textViewCityWeather?.text = sb
                    binding?.textViewCityTemp?.text =
                        "${(weather.main?.temp?.minus(273.15))?.toInt().toString()}${resources.getString(R.string.str_celsius)}"
                    val min_temp = (weather.main?.tempMin?.minus(273.15))?.toInt().toString()
                    val max_temp = (weather.main?.tempMax?.minus(273.15))?.toInt().toString()
                    binding?.textViewCityMinMaxTemp?.text = "${resources.getString(R.string.str_min)} $min_temp${resources.getString(R.string.str_celsius)} - ${resources.getString(R.string.str_max)} $max_temp${resources.getString(R.string.str_celsius)}"
                    binding?.textViewCityHumidity?.text =
                        "${resources.getString(R.string.str_humidity)} - ${weather.main?.humidity.toString()}${resources.getString(R.string.str_percent)}"
                    binding?.textViewCityPressure?.text = "${resources.getString(R.string.str_pressure)} - ${weather.main?.pressure}${resources.getString(R.string.str_mb)}"
                    binding?.textViewCityWind?.text = "${resources.getString(R.string.str_wind)} - ${weather.wind?.speed}${resources.getString(R.string.str_kmh)}"
                    val icon: String = weather.weather.get(0).icon!!

                    val iconUrl = "https://openweathermap.org/img/wn/$icon.png"
                    val myOptions = RequestOptions()
                        .override(150, 150)
                    Glide.with(this)
                        .asBitmap()
                        .apply(myOptions)
                        .load(iconUrl)
                        .into(binding?.textViewTempIcon!!)

                    val sharedPreferences =
                        getSharedPreferences(Constants.SEARCH_CITY, MODE_PRIVATE)
                    val myEdit = sharedPreferences.edit()
                    myEdit.putString(Constants.SEARCH_CITY_NAME, weather.name)
                    myEdit.apply()
                } else {
                    binding?.weatherView?.visibility = View.GONE
                    binding?.SearchInputLayout?.isErrorEnabled = true
                    binding?.SearchInputLayout?.error =
                        resources.getString(R.string.us_city_validate)
                }
            } else {
                binding?.SearchInputLayout?.isErrorEnabled = true
                binding?.SearchInputLayout?.error = resources.getString(R.string.city_name_validate)
            }

            binding?.SearchInputLayout?.setEndIconOnClickListener {
                mainViewModel.getWeatherData(
                    binding?.SearchInputEdittext?.text.toString()
                )
            }

            binding?.SearchInputEdittext?.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable) {

                }

                override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
                ) {
                    if (binding?.SearchInputLayout!!.isErrorEnabled) {
                        binding?.SearchInputLayout?.isErrorEnabled = false
                        binding?.SearchInputLayout?.error = null
                    }

                }
            })
        })

        binding?.SearchInputEdittext?.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                hideKeyboard(v)
            }
        }

        binding?.SearchInputEdittext?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                mainViewModel.getWeatherData(
                    binding?.SearchInputEdittext?.text.toString()
                )
                return@setOnEditorActionListener true
            }
            false
        }

    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            locationPermissionCode
        )
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentCity() {
        val client: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        client.lastLocation.addOnSuccessListener { location ->
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses =
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                mainViewModel.getWeatherData(addresses[0].locality)

            } catch (e: Exception) {
                Log.d("WeatherApp", "Unable to find the city.")
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentCity()
            } else {
                mainViewModel.getWeatherData(lastSearchCity)
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager: InputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}