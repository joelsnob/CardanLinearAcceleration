package com.kircherelectronics.cardanlinearacceleration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Calendar;
import com.androidplot.xy.XYPlot;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kircherelectronics.cardanlinearacceleration.R;
import com.kircherelectronics.cardanlinearacceleration.dialog.SettingsDialog;
import com.kircherelectronics.cardanlinearacceleration.filters.CardanLinearAcceleration;
import com.kircherelectronics.cardanlinearacceleration.filters.LPFWikipedia;
import com.kircherelectronics.cardanlinearacceleration.filters.LowPassFilter;
import com.kircherelectronics.cardanlinearacceleration.filters.MeanFilter;
import com.kircherelectronics.cardanlinearacceleration.gauge.GaugeAccelerationHolo;
import com.kircherelectronics.cardanlinearacceleration.gauge.GaugeRotationHolo;
import com.kircherelectronics.cardanlinearacceleration.plot.DynamicPlot;
import com.kircherelectronics.cardanlinearacceleration.plot.PlotColor;

/*
 * Low-Pass Linear Acceleration
 * Copyright (C) 2013, Kaleb Kircher - Boki Software, Kircher Engineering, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Implements an Activity that is intended to run low-pass filters on
 * accelerometer inputs and then graph the outputs.
 * 
 * @author Kaleb
 * @version %I%, %G%
 */
public class CardanLinearAccelerationActivity extends Activity implements
		SensorEventListener, Runnable, OnTouchListener
{

	// The size of the sample window that determines RMS Amplitude Noise
	// (standard deviation)
	private final static int SAMPLE_WINDOW = 50;

	// Indicate if the output should be logged to a .csv file
	private boolean logData = false;

	private boolean lpfAccelerationStaticAlpha = false;
	private boolean lpfMagneticStaticAlpha = false;;

	// Decimal formats for the UI outputs
	private DecimalFormat df;

	// Graph plot for the UI outputs
	private DynamicPlot dynamicPlot;

	// Touch to zoom constants for the dynamicPlot
	private float distance = 0;
	private float zoom = 1.2f;

	// Outputs for the acceleration and LPFs
	private float[] acceleration = new float[3];
	private float[] linearAcceleration = new float[3];
	private float[] magnetic = new float[3];
	private float[] g = new float[3];

	// The Acceleration Gauge
	private GaugeRotationHolo gaugeAccelerationTilt;

	// The LPF Gauge
	private GaugeRotationHolo gaugeLinearAccelTilt;

	// The Acceleration Gauge
	private GaugeAccelerationHolo gaugeAcceleration;

	// The LPF Gauge
	private GaugeAccelerationHolo gaugeLinearAcceleration;

	// Handler for the UI plots so everything plots smoothly
	private Handler handler;

	// Icon to indicate logging is active
	private ImageView iconLogger;

	private float accelerationLPFAlpha;
	private float magneticLPFAlpha;

	private int accelerationMeanFilterWindow;
	private int magneticMeanFilterWindow;

	// The generation of the log output
	private int generation = 0;

	// Plot keys for the acceleration plot
	private int plotAccelXAxisKey = 0;
	private int plotAccelYAxisKey = 1;
	private int plotAccelZAxisKey = 2;

	// Plot keys for the LPF Wikipedia plot
	private int plotLinearAccelXAxisKey = 3;
	private int plotLinearAccelYAxisKey = 4;
	private int plotLinearAccelZAxisKey = 5;

	// Color keys for the acceleration plot
	private int plotAccelXAxisColor;
	private int plotAccelYAxisColor;
	private int plotAccelZAxisColor;

	// Color keys for the LPF Wikipedia plot
	private int plotLinearAccelXAxisColor;
	private int plotLinearAccelYAxisColor;
	private int plotLinearAccelZAxisColor;

	// Log output time stamp
	private long logTime = 0;

	// Low-Pass Filters
	private CardanLinearAcceleration cardanLinearAcceleration;

	private LowPassFilter lpfAcceleration;
	private LowPassFilter lpfMagnetic;

	private MeanFilter meanFilterAcceleration;
	private MeanFilter meanFilterMagnetic;

	// Plot colors
	private PlotColor color;

	// Sensor manager to access the accelerometer sensor
	private SensorManager sensorManager;

	private SettingsDialog settingsDialog;

	// Acceleration plot titles
	private String plotAccelXAxisTitle = "AX";
	private String plotAccelYAxisTitle = "AY";
	private String plotAccelZAxisTitle = "AZ";

	// LPF Wikipedia plot titles
	private String plotLinearAccelXAxisTitle = "WX";
	private String plotLinearAccelYAxisTitle = "WY";
	private String plotLinearAccelZAxisTitle = "WZ";

	// Output log
	private String log;

	// Acceleration UI outputs
	private TextView xAxis;
	private TextView yAxis;
	private TextView zAxis;

	/**
	 * Get the sample window size for the standard deviation.
	 * 
	 * @return Sample window size for the standard deviation.
	 */
	public static int getSampleWindow()
	{
		return SAMPLE_WINDOW;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.plot_sensor_activity);

		// Read in the saved prefs
		readPrefs();

		View view = findViewById(R.id.ScrollView01);
		view.setOnTouchListener(this);

		// Create the graph plot
		XYPlot plot = (XYPlot) findViewById(R.id.plot_sensor);
		plot.setTitle("Acceleration");
		dynamicPlot = new DynamicPlot(plot);
		dynamicPlot.setMaxRange(1.2);
		dynamicPlot.setMinRange(-1.2);

		// Create the acceleration UI outputs
		xAxis = (TextView) findViewById(R.id.value_x_axis);
		yAxis = (TextView) findViewById(R.id.value_y_axis);
		zAxis = (TextView) findViewById(R.id.value_z_axis);

		// Create the logger icon
		iconLogger = (ImageView) findViewById(R.id.icon_logger);
		iconLogger.setVisibility(View.INVISIBLE);

		// Format the UI outputs so they look nice
		df = new DecimalFormat("#.##");

		// Get the sensor manager ready
		sensorManager = (SensorManager) this
				.getSystemService(Context.SENSOR_SERVICE);

		initFilters();

		// Create the low-pass filters
		cardanLinearAcceleration = new CardanLinearAcceleration(this,
				lpfAcceleration, lpfMagnetic, meanFilterAcceleration,
				meanFilterMagnetic);

		// Initialize the plots
		initColor();
		initAccelerationPlot();
		initGauges();

		handler = new Handler();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		sensorManager.unregisterListener(this);

		if (logData)
		{
			writeLogToFile();
		}

		handler.removeCallbacks(this);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		readPrefs();

		handler.post(this);

		// Register for sensor updates.
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);

		// Register for sensor updates.
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			// Get a local copy of the sensor values
			System.arraycopy(event.values, 0, acceleration, 0,
					event.values.length);
		}

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
		{
			// Get a local copy of the sensor values
			System.arraycopy(event.values, 0, magnetic, 0, event.values.length);
		}

		linearAcceleration = cardanLinearAcceleration.addSamples(acceleration,
				magnetic);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings_logger_menu, menu);
		return true;
	}

	/**
	 * Event Handling for Individual menu item selected Identify single menu
	 * item by it's id
	 * */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{

		// Log the data
		case R.id.menu_settings_logger_plotdata:
			startDataLog();
			return true;

			// Log the data
		case R.id.menu_settings_filter:
			showSettingsDialog();
			return true;

			// Log the data
		case R.id.menu_settings_help:
			showHelpDialog();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Pinch to zoom.
	 */
	@Override
	public boolean onTouch(View v, MotionEvent e)
	{
		// MotionEvent reports input details from the touch screen
		// and other input controls.
		float newDist = 0;

		switch (e.getAction())
		{

		case MotionEvent.ACTION_MOVE:

			// pinch to zoom
			if (e.getPointerCount() == 2)
			{
				if (distance == 0)
				{
					distance = fingerDist(e);
				}

				newDist = fingerDist(e);

				zoom *= distance / newDist;

				dynamicPlot.setMaxRange(zoom * Math.log(zoom));
				dynamicPlot.setMinRange(-zoom * Math.log(zoom));

				distance = newDist;
			}
		}

		return false;
	}

	/**
	 * Output and logs are run on their own thread to keep the UI from hanging
	 * and the output smooth.
	 */
	@Override
	public void run()
	{
		handler.postDelayed(this, 100);

		plotData();
		logData();
	}

	/**
	 * Create the plot colors.
	 */
	private void initColor()
	{
		color = new PlotColor(this);

		plotAccelXAxisColor = color.getDarkBlue();
		plotAccelYAxisColor = color.getDarkGreen();
		plotAccelZAxisColor = color.getDarkRed();

		plotLinearAccelXAxisColor = color.getMidBlue();
		plotLinearAccelYAxisColor = color.getMidGreen();
		plotLinearAccelZAxisColor = color.getMidRed();
	}

	/**
	 * Create the output graph line chart.
	 */
	private void initAccelerationPlot()
	{
		addPlot(plotAccelXAxisTitle, plotAccelXAxisKey, plotAccelXAxisColor);
		addPlot(plotAccelYAxisTitle, plotAccelYAxisKey, plotAccelYAxisColor);
		addPlot(plotAccelZAxisTitle, plotAccelZAxisKey, plotAccelZAxisColor);

		addPlot(plotLinearAccelXAxisTitle, plotLinearAccelXAxisKey,
				plotLinearAccelXAxisColor);
		addPlot(plotLinearAccelYAxisTitle, plotLinearAccelYAxisKey,
				plotLinearAccelYAxisColor);
		addPlot(plotLinearAccelZAxisTitle, plotLinearAccelZAxisKey,
				plotLinearAccelZAxisColor);
	}

	private void initFilters()
	{
		lpfAcceleration = new LPFWikipedia();
		lpfAcceleration.setAlphaStatic(lpfAccelerationStaticAlpha);
		lpfAcceleration.setAlpha(accelerationLPFAlpha);

		lpfMagnetic = new LPFWikipedia();
		lpfMagnetic.setAlphaStatic(lpfMagneticStaticAlpha);
		lpfMagnetic.setAlpha(magneticLPFAlpha);

		meanFilterAcceleration = new MeanFilter();
		meanFilterAcceleration.setWindowSize(accelerationMeanFilterWindow);

		meanFilterMagnetic = new MeanFilter();
		meanFilterMagnetic.setWindowSize(magneticMeanFilterWindow);
	}

	/**
	 * Create the RMS Noise bar chart.
	 */
	private void initGauges()
	{
		gaugeAccelerationTilt = (GaugeRotationHolo) findViewById(R.id.gauge_acceleration_tilt);
		gaugeLinearAccelTilt = (GaugeRotationHolo) findViewById(R.id.gauge_linear_acceleration_tilt);

		gaugeAcceleration = (GaugeAccelerationHolo) findViewById(R.id.gauge_acceleration);
		gaugeLinearAcceleration = (GaugeAccelerationHolo) findViewById(R.id.gauge_linear_acceleration);
	}

	/**
	 * Add a plot to the graph.
	 * 
	 * @param title
	 *            The name of the plot.
	 * @param key
	 *            The unique plot key
	 * @param color
	 *            The color of the plot
	 */
	private void addPlot(String title, int key, int color)
	{
		dynamicPlot.addSeriesPlot(title, key, color);
	}

	/**
	 * Remove a plot from the graph.
	 * 
	 * @param key
	 */
	private void removePlot(int key)
	{
		dynamicPlot.removeSeriesPlot(key);
	}

	private void showHelpDialog()
	{
		Dialog helpDialog = new Dialog(this);
		helpDialog.setCancelable(true);
		helpDialog.setCanceledOnTouchOutside(true);

		helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		helpDialog.setContentView(getLayoutInflater().inflate(R.layout.help,
				null));

		helpDialog.show();
	}

	/**
	 * Show a settings dialog.
	 */
	private void showSettingsDialog()
	{
		if (settingsDialog == null)
		{
			settingsDialog = new SettingsDialog(this, cardanLinearAcceleration,
					lpfAcceleration, lpfMagnetic, meanFilterAcceleration,
					meanFilterMagnetic);
			settingsDialog.setCancelable(true);
			settingsDialog.setCanceledOnTouchOutside(true);
		}

		settingsDialog.show();
	}

	/**
	 * Begin logging data to an external .csv file.
	 */
	private void startDataLog()
	{
		if (logData == false)
		{
			CharSequence text = "Logging Data";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.show();

			String headers = "Generation" + ",";

			headers += "Timestamp" + ",";

			headers += this.plotAccelXAxisTitle + ",";

			headers += this.plotAccelYAxisTitle + ",";

			headers += this.plotAccelZAxisTitle + ",";

			headers += this.plotLinearAccelXAxisTitle + ",";

			headers += this.plotLinearAccelYAxisTitle + ",";

			headers += this.plotLinearAccelZAxisTitle + ",";

			log = headers + "\n";

			iconLogger.setVisibility(View.VISIBLE);

			logData = true;
		}
		else
		{
			iconLogger.setVisibility(View.INVISIBLE);

			logData = false;
			writeLogToFile();
		}
	}

	/**
	 * Plot the output data in the UI.
	 */
	private void plotData()
	{
		g[0] = acceleration[0] / SensorManager.GRAVITY_EARTH;
		g[1] = acceleration[1] / SensorManager.GRAVITY_EARTH;
		g[2] = acceleration[2] / SensorManager.GRAVITY_EARTH;

		dynamicPlot.setData(g[0], plotAccelXAxisKey);
		dynamicPlot.setData(g[1], plotAccelYAxisKey);
		dynamicPlot.setData(g[2], plotAccelZAxisKey);

		dynamicPlot.setData(linearAcceleration[0], plotLinearAccelXAxisKey);
		dynamicPlot.setData(linearAcceleration[1], plotLinearAccelYAxisKey);
		dynamicPlot.setData(linearAcceleration[2], plotLinearAccelZAxisKey);

		dynamicPlot.draw();

		// Update the view with the new acceleration data
		xAxis.setText(df.format(g[0]));
		yAxis.setText(df.format(g[1]));
		zAxis.setText(df.format(g[2]));

		gaugeAccelerationTilt.updateRotation(g);
		gaugeLinearAccelTilt.updateRotation(linearAcceleration);

		gaugeAcceleration.updatePoint(acceleration[0], acceleration[1],
				Color.parseColor("#33b5e5"));

		gaugeLinearAcceleration.updatePoint(linearAcceleration[0]
				* SensorManager.GRAVITY_EARTH, linearAcceleration[1]
				* SensorManager.GRAVITY_EARTH, Color.parseColor("#33b5e5"));
	}

	/**
	 * Log output data to an external .csv file.
	 */
	private void logData()
	{
		if (logData)
		{
			if (generation == 0)
			{
				logTime = System.currentTimeMillis();
			}

			log += System.getProperty("line.separator");
			log += generation++ + ",";
			log += System.currentTimeMillis() - logTime + ",";

			log += acceleration[0] + ",";
			log += acceleration[1] + ",";
			log += acceleration[2] + ",";

			log += linearAcceleration[0] + ",";
			log += linearAcceleration[1] + ",";
			log += linearAcceleration[2] + ",";
		}
	}

	/**
	 * Write the logged data out to a persisted file.
	 */
	private void writeLogToFile()
	{
		Calendar c = Calendar.getInstance();
		String filename = "CardanLinearAcceleration-" + c.get(Calendar.YEAR)
				+ "-" + c.get(Calendar.DAY_OF_WEEK_IN_MONTH) + "-"
				+ c.get(Calendar.HOUR) + "-" + c.get(Calendar.HOUR) + "-"
				+ c.get(Calendar.MINUTE) + "-" + c.get(Calendar.SECOND)
				+ ".csv";

		File dir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "LinearAcceleration" + File.separator
				+ "Logs" + File.separator + "Acceleration");
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		File file = new File(dir, filename);

		FileOutputStream fos;
		byte[] data = log.getBytes();
		try
		{
			fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
			fos.close();

			CharSequence text = "Log Saved";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
		}
		catch (FileNotFoundException e)
		{
			CharSequence text = e.toString();
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
		}
		catch (IOException e)
		{
			// handle exception
		}
		finally
		{
			this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
					.parse("file://"
							+ Environment.getExternalStorageDirectory())));
		}
	}

	/**
	 * Get the distance between fingers for the touch to zoom.
	 * 
	 * @param event
	 * @return
	 */
	private final float fingerDist(MotionEvent event)
	{
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	/**
	 * Read in the current user preferences.
	 */
	private void readPrefs()
	{

		SharedPreferences prefs = this.getSharedPreferences("filter_prefs",
				Activity.MODE_PRIVATE);

		this.lpfAccelerationStaticAlpha = prefs.getBoolean(
				"lpf_acceleration_static_alpha", false);
		this.lpfMagneticStaticAlpha = prefs.getBoolean(
				"lpf_magnetic_static_alpha", false);

		this.accelerationLPFAlpha = prefs.getFloat(
				"lpf_acceleration_static_alpha_value", 0.4f);
		this.magneticLPFAlpha = prefs.getFloat(
				"lpf_magnetic_static_alpha_value", 0.1f);

		this.accelerationMeanFilterWindow = prefs.getInt(
				"mean_filter_acceleration_window_value", 10);
		this.magneticMeanFilterWindow = prefs.getInt(
				"mean_filter_magnetic_window_value", 10);
	}
}
