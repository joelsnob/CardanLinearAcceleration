package com.kircherelectronics.cardanlinearacceleration.dialog;

import java.text.DecimalFormat;

import com.kircherelectronics.cardanlinearacceleration.CardanLinearAccelerationActivity;
import com.kircherelectronics.cardanlinearacceleration.R;
import com.kircherelectronics.cardanlinearacceleration.R.id;
import com.kircherelectronics.cardanlinearacceleration.R.layout;
import com.kircherelectronics.cardanlinearacceleration.filters.CardanLinearAcceleration;
import com.kircherelectronics.cardanlinearacceleration.filters.LowPassFilter;
import com.kircherelectronics.cardanlinearacceleration.filters.MeanFilter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;

/*
 * Acceleration Filter
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
 * A special dialog for the settings of the application. Allows the user to
 * select what filters are plotted and set filter parameters.
 * 
 * @author Kaleb
 * @version %I%, %G%
 */
public class SettingsDialog extends Dialog implements
		NumberPicker.OnValueChangeListener, OnCheckedChangeListener
{
	private boolean showAccelerationSetAlpha = false;
	private boolean showMagneticSetAlpha = false;

	private boolean lpfAccelerationActive = false;
	private boolean lpfMagneticActive = false;

	private boolean meanFilterAccelerationActive = false;
	private boolean meanFilterMagneticActive = false;

	private LayoutInflater inflater;

	private View settingsFiltersAccelerationView;
	private View settingsFiltersMagneticView;

	private View settingsLPFAccelerationDynamicAlphaView;
	private View settingsLPFMagneticDynamicAlphaView;

	private View settingsLPFAccelerationSetAlphaView;
	private View settingsMeanFilterAccelerationSetWindowView;

	private View settingsLPFMagneticSetAlphaView;
	private View settingsMeanFilterMagneticSetWindowView;

	private View settingsLPFAccelerationToggleSetAlphaView;
	private View settingsLPFMagneticToggleSetAlphaView;

	private NumberPicker lpfAccelerationAlphaNP;
	private NumberPicker lpfMagneticAlphaNP;

	private NumberPicker meanFilterAccelerationWindowNP;
	private NumberPicker meanFilterMagneticWindowNP;

	private TextView accelerationFilterTextView;
	private TextView accelerationAlphaTextView;
	private TextView accelerationWindowTextView;

	private TextView magneticFilterTextView;
	private TextView magneticAlphaTextView;
	private TextView magneticWindowTextView;

	private DecimalFormat df;

	private CheckBox accelerationLPFSetAlphaCheckBox;

	private CheckBox accelerationLPFActiveCheckBox;

	private CheckBox accelerationMeanFilterActiveCheckBox;

	private CheckBox magneticLPFSetAlphaCheckBox;

	private CheckBox magneticLPFActiveCheckBox;

	private CheckBox magneticMeanFilterActiveCheckBox;

	private RelativeLayout accelerationLPFSetAlphaView;

	private RelativeLayout accelerationMeanFilterSetWindowView;

	private RelativeLayout magneticLPFSetAlphaView;

	private RelativeLayout magneticMeanFilterSetWindowView;

	private RelativeLayout accelerationLPFToggleSetAlphaView;

	private RelativeLayout magneticLPFToggleSetAlphaView;

	private LowPassFilter lpfAcceleration;

	private LowPassFilter lpfMagnetic;

	private MeanFilter meanFilterAcceleration;

	private MeanFilter meanFilterMagnetic;

	private float accelerationLPFAlpha;
	private float magneticLPFAlpha;
	private int accelerationMeanFilterWindow;
	private int magneticMeanFilterWindow;

	private CardanLinearAcceleration sensorFusion;

	/**
	 * Create a dialog.
	 * 
	 * @param context
	 *            The context.
	 * @param lpfAcceleration
	 *            The Wikipedia LPF.
	 * @param lpfMagnetic
	 *            The Android Developer LPF.
	 */
	public SettingsDialog(Context context,
			CardanLinearAcceleration sensorFusion,
			LowPassFilter lpfAcceleration, LowPassFilter lpfMagnetic,
			MeanFilter meanFilterAcceleration, MeanFilter meanFilterMagnetic)
	{
		super(context);

		this.sensorFusion = sensorFusion;

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		this.lpfAcceleration = lpfAcceleration;
		this.lpfMagnetic = lpfMagnetic;
		this.meanFilterAcceleration = meanFilterAcceleration;
		this.meanFilterMagnetic = meanFilterMagnetic;

		readPrefs();

		inflater = getLayoutInflater();

		View settingsView = inflater.inflate(R.layout.settings, null, false);

		LinearLayout layout = (LinearLayout) settingsView
				.findViewById(R.id.layout_settings_content);

		createAccelerationFilterSettings();
		createMagneticFilterSettings();

		layout.addView(settingsFiltersAccelerationView);
		layout.addView(settingsFiltersMagneticView);

		this.setContentView(settingsView);

		df = new DecimalFormat("#.####");
	}

	@Override
	public void onStop()
	{
		super.onStop();

		writePrefs();
	}

	@Override
	public void onValueChange(NumberPicker picker, int oldVal, int newVal)
	{
		if (picker.equals(lpfAccelerationAlphaNP))
		{
			accelerationAlphaTextView.setText(df.format(newVal * 0.001));

			if (showAccelerationSetAlpha)
			{
				accelerationLPFAlpha = newVal * 0.001f;

				lpfAcceleration.setAlpha(accelerationLPFAlpha);
			}
		}

		if (picker.equals(lpfMagneticAlphaNP))
		{
			magneticAlphaTextView.setText(df.format(newVal * 0.001));

			if (showMagneticSetAlpha)
			{
				magneticLPFAlpha = newVal * 0.001f;

				lpfMagnetic.setAlpha(magneticLPFAlpha);
			}
		}

		if (picker.equals(meanFilterAccelerationWindowNP))
		{
			accelerationWindowTextView.setText(df.format(newVal));

			accelerationMeanFilterWindow = newVal;

			meanFilterAcceleration.setWindowSize(accelerationMeanFilterWindow);
		}

		if (picker.equals(meanFilterMagneticWindowNP))
		{
			magneticWindowTextView.setText(df.format(newVal));

			magneticMeanFilterWindow = newVal;

			meanFilterMagnetic.setWindowSize(magneticMeanFilterWindow);
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		if (buttonView.equals(this.accelerationLPFActiveCheckBox))
		{
			if (isChecked)
			{
				lpfAccelerationActive = true;

				showAccelerationToggleSetLPFAlphaView();

			}
			else
			{
				lpfAccelerationActive = false;

				removeAccelerationToggleSetLPFAlphaView();
			}

			this.sensorFusion.setLpfAccelerationActive(lpfAccelerationActive);
		}

		if (buttonView.equals(this.accelerationLPFSetAlphaCheckBox))
		{
			if (isChecked)
			{
				showAccelerationSetAlpha = true;

				showAccelerationSetLPFAlphaView();

				lpfAcceleration.setAlphaStatic(showAccelerationSetAlpha);
			}
			else
			{
				showAccelerationSetAlpha = false;

				removeAccelerationSetLPFAlphaView();

				lpfAcceleration.setAlphaStatic(showAccelerationSetAlpha);
			}
		}

		if (buttonView.equals(this.accelerationMeanFilterActiveCheckBox))
		{
			if (isChecked)
			{
				meanFilterAccelerationActive = true;

				showAccelerationSetMeanFilterWindow();
			}
			else
			{
				meanFilterAccelerationActive = false;

				removeAccelerationSetMeanFilterWindow();
			}

			this.sensorFusion
					.setMeanFilterAccelerationActive(meanFilterAccelerationActive);
		}

		if (buttonView.equals(this.magneticLPFActiveCheckBox))
		{
			if (isChecked)
			{
				lpfMagneticActive = true;

				showMagneticToggleSetLPFAlphaView();

			}
			else
			{
				lpfMagneticActive = false;

				removeMagneticToggleSetLPFAlphaView();
			}

			this.sensorFusion.setLpfMagneticActive(lpfMagneticActive);
		}

		if (buttonView.equals(this.magneticLPFSetAlphaCheckBox))
		{
			if (isChecked)
			{
				showMagneticSetAlpha = true;

				showMagneticSetLPFAlphaView();

				lpfMagnetic.setAlphaStatic(showMagneticSetAlpha);
			}
			else
			{
				showMagneticSetAlpha = false;

				removeMagneticSetLPFAlphaView();

				lpfMagnetic.setAlphaStatic(showMagneticSetAlpha);
			}
		}

		if (buttonView.equals(this.magneticMeanFilterActiveCheckBox))
		{
			if (isChecked)
			{
				meanFilterMagneticActive = true;

				showMagneticSetMeanFilterWindow();
			}
			else
			{
				meanFilterMagneticActive = false;

				removeMagneticSetMeanFilterWindow();
			}

			this.sensorFusion
					.setMeanFilterMagneticActive(meanFilterMagneticActive);
		}
	}

	private void createAccelerationFilterSettings()
	{
		settingsFiltersAccelerationView = inflater.inflate(
				R.layout.settings_filter, null, false);

		settingsLPFAccelerationDynamicAlphaView = inflater.inflate(
				R.layout.settings_toggle_set_value, null, false);

		accelerationLPFActiveCheckBox = (CheckBox) settingsFiltersAccelerationView
				.findViewById(R.id.check_box_lpf);

		accelerationLPFActiveCheckBox.setOnCheckedChangeListener(this);

		if (lpfAccelerationActive)
		{
			accelerationLPFActiveCheckBox.setChecked(true);
		}
		else
		{
			accelerationLPFActiveCheckBox.setChecked(false);
		}

		accelerationMeanFilterActiveCheckBox = (CheckBox) settingsFiltersAccelerationView
				.findViewById(R.id.check_box_mean_filter);

		accelerationMeanFilterActiveCheckBox.setOnCheckedChangeListener(this);

		if (meanFilterAccelerationActive)
		{
			accelerationMeanFilterActiveCheckBox.setChecked(true);
		}
		else
		{
			accelerationMeanFilterActiveCheckBox.setChecked(false);
		}

		accelerationFilterTextView = (TextView) settingsFiltersAccelerationView
				.findViewById(R.id.label_filter_name);

		accelerationFilterTextView.setText("Acceleration");

		showAccelerationToggleSetLPFAlphaView();
	}

	private void createMagneticFilterSettings()
	{
		settingsFiltersMagneticView = inflater.inflate(
				R.layout.settings_filter, null, false);

		settingsLPFMagneticDynamicAlphaView = inflater.inflate(
				R.layout.settings_toggle_set_value, null, false);

		magneticLPFActiveCheckBox = (CheckBox) settingsFiltersMagneticView
				.findViewById(R.id.check_box_lpf);

		magneticLPFActiveCheckBox.setOnCheckedChangeListener(this);

		if (lpfMagneticActive)
		{
			magneticLPFActiveCheckBox.setChecked(true);
		}
		else
		{
			magneticLPFActiveCheckBox.setChecked(false);
		}

		magneticMeanFilterActiveCheckBox = (CheckBox) settingsFiltersMagneticView
				.findViewById(R.id.check_box_mean_filter);

		magneticMeanFilterActiveCheckBox.setOnCheckedChangeListener(this);

		if (meanFilterMagneticActive)
		{
			magneticMeanFilterActiveCheckBox.setChecked(true);
		}
		else
		{
			magneticMeanFilterActiveCheckBox.setChecked(false);
		}

		magneticFilterTextView = (TextView) settingsFiltersMagneticView
				.findViewById(R.id.label_filter_name);

		magneticFilterTextView.setText("Magnetic");

		showAccelerationToggleSetLPFAlphaView();
	}

	/**
	 * Create the Android Developer Settings.
	 */
	private void showAccelerationToggleSetLPFAlphaView()
	{
		if (lpfAccelerationActive)
		{
			if (settingsLPFAccelerationToggleSetAlphaView == null)
			{
				settingsLPFAccelerationToggleSetAlphaView = inflater.inflate(
						R.layout.settings_toggle_set_value, null, false);
			}

			accelerationLPFSetAlphaCheckBox = (CheckBox) settingsLPFAccelerationToggleSetAlphaView
					.findViewById(R.id.check_box_static_alpha);

			accelerationLPFSetAlphaCheckBox.setOnCheckedChangeListener(this);

			if (showAccelerationSetAlpha)
			{
				accelerationLPFSetAlphaCheckBox.setChecked(true);
			}
			else
			{
				accelerationLPFSetAlphaCheckBox.setChecked(false);
			}

			accelerationLPFToggleSetAlphaView = (RelativeLayout) settingsFiltersAccelerationView
					.findViewById(R.id.layout_toggle_lpf_values);

			accelerationLPFToggleSetAlphaView.removeAllViews();

			accelerationLPFToggleSetAlphaView
					.addView(settingsLPFAccelerationToggleSetAlphaView);
		}
	}

	/**
	 * Show the Android Developer Settings.
	 */
	private void showAccelerationSetLPFAlphaView()
	{
		if (showAccelerationSetAlpha)
		{
			if (settingsLPFAccelerationSetAlphaView == null)
			{
				settingsLPFAccelerationSetAlphaView = inflater.inflate(
						R.layout.settings_filter_set_value, null, false);
			}

			accelerationAlphaTextView = (TextView) settingsLPFAccelerationSetAlphaView
					.findViewById(R.id.value);
			accelerationAlphaTextView.setText(String.valueOf(accelerationLPFAlpha));
			
			TextView accelerationLabelAlphaTextView = (TextView) settingsLPFAccelerationSetAlphaView
					.findViewById(R.id.label_value);
			accelerationLabelAlphaTextView.setText("Alpha:");

			lpfAccelerationAlphaNP = (NumberPicker) settingsLPFAccelerationSetAlphaView
					.findViewById(R.id.numberPicker1);
			lpfAccelerationAlphaNP.setMaxValue(1000);
			lpfAccelerationAlphaNP.setMinValue(0);
			lpfAccelerationAlphaNP.setValue((int) (accelerationLPFAlpha*100));

			lpfAccelerationAlphaNP.setOnValueChangedListener(this);

			accelerationLPFSetAlphaView = (RelativeLayout) settingsFiltersAccelerationView
					.findViewById(R.id.layout_set_lpf_values);

			accelerationLPFSetAlphaView
					.addView(settingsLPFAccelerationSetAlphaView);
		}
	}

	private void showAccelerationSetMeanFilterWindow()
	{
		if (meanFilterAccelerationActive)
		{
			if (settingsMeanFilterAccelerationSetWindowView == null)
			{
				settingsMeanFilterAccelerationSetWindowView = inflater.inflate(
						R.layout.settings_filter_set_value, null, false);
			}

			accelerationWindowTextView = (TextView) settingsMeanFilterAccelerationSetWindowView
					.findViewById(R.id.value);
			accelerationWindowTextView.setText(String.valueOf(accelerationMeanFilterWindow));

			meanFilterAccelerationWindowNP = (NumberPicker) settingsMeanFilterAccelerationSetWindowView
					.findViewById(R.id.numberPicker1);
			meanFilterAccelerationWindowNP.setMaxValue(100);
			meanFilterAccelerationWindowNP.setMinValue(0);
			meanFilterAccelerationWindowNP.setValue(accelerationMeanFilterWindow);

			meanFilterAccelerationWindowNP.setOnValueChangedListener(this);

			accelerationMeanFilterSetWindowView = (RelativeLayout) settingsFiltersAccelerationView
					.findViewById(R.id.layout_set_mean_filter_values);

			accelerationMeanFilterSetWindowView
					.addView(settingsMeanFilterAccelerationSetWindowView);
		}
	}

	/**
	 * Create the Android Developer Settings.
	 */
	private void showMagneticToggleSetLPFAlphaView()
	{
		if (lpfMagneticActive)
		{
			if (settingsLPFMagneticToggleSetAlphaView == null)
			{
				settingsLPFMagneticToggleSetAlphaView = inflater.inflate(
						R.layout.settings_toggle_set_value, null, false);
			}

			magneticLPFSetAlphaCheckBox = (CheckBox) settingsLPFMagneticToggleSetAlphaView
					.findViewById(R.id.check_box_static_alpha);

			magneticLPFSetAlphaCheckBox.setOnCheckedChangeListener(this);

			if (showMagneticSetAlpha)
			{
				magneticLPFSetAlphaCheckBox.setChecked(true);
			}
			else
			{
				magneticLPFSetAlphaCheckBox.setChecked(false);
			}

			magneticLPFToggleSetAlphaView = (RelativeLayout) settingsFiltersMagneticView
					.findViewById(R.id.layout_toggle_lpf_values);

			magneticLPFToggleSetAlphaView.removeAllViews();

			magneticLPFToggleSetAlphaView
					.addView(settingsLPFMagneticToggleSetAlphaView);
		}
	}

	/**
	 * Show the Android Developer Settings.
	 */
	private void showMagneticSetLPFAlphaView()
	{
		if (showMagneticSetAlpha)
		{
			if (settingsLPFMagneticSetAlphaView == null)
			{
				settingsLPFMagneticSetAlphaView = inflater.inflate(
						R.layout.settings_filter_set_value, null, false);
			}

			magneticAlphaTextView = (TextView) settingsLPFMagneticSetAlphaView
					.findViewById(R.id.value);
			magneticAlphaTextView.setText(String.valueOf(magneticLPFAlpha));
			

			TextView magneticLabelAlphaTextView = (TextView) settingsLPFMagneticSetAlphaView
					.findViewById(R.id.label_value);
			magneticLabelAlphaTextView.setText("Alpha:");

			lpfMagneticAlphaNP = (NumberPicker) settingsLPFMagneticSetAlphaView
					.findViewById(R.id.numberPicker1);
			lpfMagneticAlphaNP.setMaxValue(1000);
			lpfMagneticAlphaNP.setMinValue(0);
			lpfMagneticAlphaNP.setValue((int) (magneticLPFAlpha*100));

			lpfMagneticAlphaNP.setOnValueChangedListener(this);

			magneticLPFSetAlphaView = (RelativeLayout) settingsFiltersMagneticView
					.findViewById(R.id.layout_set_lpf_values);

			magneticLPFSetAlphaView.addView(settingsLPFMagneticSetAlphaView);
		}
	}

	private void showMagneticSetMeanFilterWindow()
	{
		if (meanFilterMagneticActive)
		{
			if (settingsMeanFilterMagneticSetWindowView == null)
			{
				settingsMeanFilterMagneticSetWindowView = inflater.inflate(
						R.layout.settings_filter_set_value, null, false);
			}

			magneticWindowTextView = (TextView) settingsMeanFilterMagneticSetWindowView
					.findViewById(R.id.value);
			magneticWindowTextView.setText(String.valueOf(accelerationMeanFilterWindow));

			meanFilterMagneticWindowNP = (NumberPicker) settingsMeanFilterMagneticSetWindowView
					.findViewById(R.id.numberPicker1);
			meanFilterMagneticWindowNP.setMaxValue(100);
			meanFilterMagneticWindowNP.setMinValue(0);
			meanFilterMagneticWindowNP.setValue(accelerationMeanFilterWindow);

			meanFilterMagneticWindowNP.setOnValueChangedListener(this);

			magneticMeanFilterSetWindowView = (RelativeLayout) settingsFiltersMagneticView
					.findViewById(R.id.layout_set_mean_filter_values);

			magneticMeanFilterSetWindowView
					.addView(settingsMeanFilterMagneticSetWindowView);
		}
	}

	/**
	 * Remove the Wikipedia Settings.
	 */
	private void removeAccelerationSetMeanFilterWindow()
	{
		if (!meanFilterAccelerationActive)
		{
			accelerationMeanFilterSetWindowView
					.removeView(settingsMeanFilterAccelerationSetWindowView);

			settingsFiltersAccelerationView.invalidate();
		}
	}

	/**
	 * Remove the Wikipedia Settings.
	 */
	private void removeAccelerationToggleSetLPFAlphaView()
	{
		if (!lpfAccelerationActive)
		{
			accelerationLPFToggleSetAlphaView = (RelativeLayout) settingsFiltersAccelerationView
					.findViewById(R.id.layout_toggle_lpf_values);

			accelerationLPFToggleSetAlphaView.removeAllViews();

			accelerationLPFToggleSetAlphaView.invalidate();
		}
	}

	/**
	 * Remove the Android Developer Settings.
	 */
	private void removeAccelerationSetLPFAlphaView()
	{
		if (!showAccelerationSetAlpha)
		{
			accelerationLPFSetAlphaView = (RelativeLayout) settingsFiltersAccelerationView
					.findViewById(R.id.layout_set_lpf_values);

			accelerationLPFSetAlphaView
					.removeView(settingsLPFAccelerationSetAlphaView);

			settingsFiltersAccelerationView.invalidate();
		}
	}

	/**
	 * Remove the Wikipedia Settings.
	 */
	private void removeMagneticSetMeanFilterWindow()
	{
		if (!meanFilterMagneticActive)
		{
			magneticMeanFilterSetWindowView
					.removeView(settingsMeanFilterMagneticSetWindowView);

			settingsFiltersMagneticView.invalidate();
		}
	}

	/**
	 * Remove the Wikipedia Settings.
	 */
	private void removeMagneticToggleSetLPFAlphaView()
	{
		if (!lpfMagneticActive)
		{
			magneticLPFToggleSetAlphaView = (RelativeLayout) settingsFiltersMagneticView
					.findViewById(R.id.layout_toggle_lpf_values);

			magneticLPFToggleSetAlphaView.removeAllViews();

			magneticLPFToggleSetAlphaView.invalidate();
		}
	}

	/**
	 * Remove the Android Developer Settings.
	 */
	private void removeMagneticSetLPFAlphaView()
	{
		if (!showMagneticSetAlpha)
		{
			magneticLPFSetAlphaView = (RelativeLayout) settingsFiltersMagneticView
					.findViewById(R.id.layout_set_lpf_values);

			magneticLPFSetAlphaView.removeView(settingsLPFMagneticSetAlphaView);

			settingsFiltersMagneticView.invalidate();
		}
	}

	/**
	 * Read in the current user preferences.
	 */
	private void readPrefs()
	{
		SharedPreferences prefs = this.getContext().getSharedPreferences(
				"filter_prefs", Activity.MODE_PRIVATE);
		
		this.accelerationLPFAlpha = prefs
				.getFloat("lpf_acceleration_static_alpha_value", 0.4f);
		this.magneticLPFAlpha = prefs
				.getFloat("lpf_magnetic_static_alpha_value", 0.1f);
		
		this.accelerationMeanFilterWindow = prefs
				.getInt("mean_filter_acceleration_window_value", 10);
		this.magneticMeanFilterWindow = prefs
				.getInt("mean_filter_magnetic_window_value", 10);
		
		this.showAccelerationSetAlpha = prefs
				.getBoolean("lpf_acceleration_static_alpha", false);
		this.showMagneticSetAlpha = prefs.getBoolean("lpf_magnetic_static_alpha", false);

		this.lpfAccelerationActive = prefs
				.getBoolean("lpf_acceleration", false);
		this.lpfMagneticActive = prefs.getBoolean("lpf_magnetic", false);

		this.meanFilterAccelerationActive = prefs.getBoolean(
				"mean_filter_acceleration", false);
		this.meanFilterMagneticActive = prefs.getBoolean(
				"mean_filter_magnetic", false);

	}

	/**
	 * Write the preferences.
	 */
	private void writePrefs()
	{
		// Write out the offsets to the user preferences.
		SharedPreferences.Editor editor = this.getContext()
				.getSharedPreferences("filter_prefs", Activity.MODE_PRIVATE)
				.edit();
		
		editor.putFloat("lpf_acceleration_static_alpha_value", this.accelerationLPFAlpha);
		editor.putFloat("lpf_magnetic_static_alpha_value", this.magneticLPFAlpha);
		
		editor.putInt("mean_filter_acceleration_window_value", this.accelerationMeanFilterWindow);
		editor.putInt("mean_filter_magnetic_window_value", this.magneticMeanFilterWindow);
		
		editor.putBoolean("lpf_acceleration_static_alpha", this.showAccelerationSetAlpha);
		editor.putBoolean("lpf_magnetic_static_alpha", this.showMagneticSetAlpha);

		editor.putBoolean("lpf_acceleration", this.lpfAccelerationActive);
		editor.putBoolean("lpf_magnetic", this.lpfMagneticActive);

		editor.putBoolean("mean_filter_acceleration",
				this.meanFilterAccelerationActive);
		editor.putBoolean("mean_filter_magnetic", this.meanFilterMagneticActive);

		editor.commit();
	}
}
