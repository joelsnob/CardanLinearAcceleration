package com.kircherelectronics.cardanlinearacceleration.filters;

import com.kircherelectronics.cardanlinearacceleration.statistics.StdDev;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.util.Log;

/*
 * Cardan Linear Acceleration
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
 * An implementation of a accelerometer magnetometer sensor fusion. The
 * algorithm determines the linear acceleration of the device by using Cardan
 * angles.
 * 
 * @author Kaleb
 * @see http://en.wikipedia.org/wiki/Low-pass_filter
 * @version %I%, %G%
 */
public class CardanLinearAcceleration
{

	private boolean lpfAccelerationActive = false;
	private boolean lpfMagneticActive = false;

	private boolean meanFilterAccelerationActive = false;
	private boolean meanFilterMagneticActive = false;

	// The gravity components of the acceleration signal.
	private float[] components = new float[3];

	private float[] linearAcceleration = new float[]
	{ 0, 0, 0 };

	// Raw accelerometer data
	private float[] acceleration = new float[]
	{ 0, 0, 0 };

	// Raw accelerometer data
	private float[] magnetic = new float[]
	{ 0, 0, 0 };

	private LowPassFilter lpfAcceleration;
	private LowPassFilter lpfMagnetic;

	private MeanFilter meanFilterAcceleration;
	private MeanFilter meanFilterMagnetic;

	// The rotation matrix R transforming a vector from the device
	// coordinate system to the world's coordinate system which is
	// defined as a direct orthonormal basis. R is the identity
	// matrix when the device is aligned with the world's coordinate
	// system, that is, when the device's X axis points toward East,
	// the Y axis points to the North Pole and the device is facing
	// the sky. NOTE: the reference coordinate-system used by
	// getOrientation() is different from the world
	// coordinate-system defined for the rotation matrix R and
	// getRotationMatrix().
	private float[] r = new float[9];

	private StdDev varianceAccel;

	private Context context;

	public CardanLinearAcceleration(Context context,
			LowPassFilter lpfAcceleration, LowPassFilter lpfMagnetic,
			MeanFilter meanFilterAcceleration, MeanFilter meanFilterMagnetic)
	{
		super();

		this.context = context;
		this.lpfAcceleration = lpfAcceleration;
		this.lpfMagnetic = lpfMagnetic;
		this.meanFilterAcceleration = meanFilterAcceleration;
		this.meanFilterMagnetic = meanFilterMagnetic;

		readPrefs();

		// Create the RMS Noise calculations
		varianceAccel = new StdDev();
	}

	/**
	 * Add a sample.
	 * 
	 * @param acceleration
	 *            The acceleration data.
	 * @return Returns the output of the filter.
	 */
	public float[] addSamples(float[] acceleration, float[] magnetic)
	{
		// Get a local copy of the sensor values
		System.arraycopy(acceleration, 0, this.acceleration, 0,
				acceleration.length);

		if (lpfAccelerationActive)
		{
			System.arraycopy(lpfAcceleration.addSamples(this.acceleration), 0,
					this.acceleration, 0, this.acceleration.length);
		}

		if (meanFilterAccelerationActive)
		{
			this.acceleration = meanFilterAcceleration
					.filterFloat(this.acceleration);
		}

		// Get a local copy of the sensor values
		System.arraycopy(magnetic, 0, this.magnetic, 0, magnetic.length);

		if (lpfMagneticActive)
		{
			System.arraycopy(lpfMagnetic.addSamples(this.magnetic), 0,
					this.magnetic, 0, this.magnetic.length);
		}

		if (meanFilterMagneticActive)
		{
			this.magnetic = meanFilterMagnetic.filterFloat(this.magnetic);
		}

		// Get the rotation matrix to put our local device coordinates
		// into the world-coordinate system.
		if (SensorManager.getRotationMatrix(r, null, this.acceleration,
				this.magnetic))
		{
			// values[0]: azimuth/yaw, rotation around the Z axis.
			// values[1]: pitch, rotation around the X axis.
			// values[2]: roll, rotation around the Y axis.
			float[] values = new float[3];

			// NOTE: the reference coordinate-system used is different
			// from the world coordinate-system defined for the rotation
			// matrix:
			// X is defined as the vector product Y.Z (It is tangential
			// to the ground at the device's current location and
			// roughly points West). Y is tangential to the ground at
			// the device's current location and points towards the
			// magnetic North Pole. Z points towards the center of the
			// Earth and is perpendicular to the ground.
			SensorManager.getOrientation(r, values);

			float magnitude = (float) (Math.sqrt(Math.pow(this.acceleration[0],
					2)
					+ Math.pow(this.acceleration[1], 2)
					+ Math.pow(this.acceleration[2], 2)) / SensorManager.GRAVITY_EARTH);

			double var = varianceAccel.addSample(magnitude);

			// Attempt to estimate the gravity components when the device is
			// stable and not experiencing linear acceleration.
			if (var < 0.03)
			{
				// Find the gravity component of the X-axis
				// = -g*sin(roll);
				components[0] = (float) (-SensorManager.GRAVITY_EARTH * Math
						.sin(values[2]));

				// Find the gravity component of the Y-axis
				// = -g*cos(roll)*sin(pith);
				components[1] = (float) (-SensorManager.GRAVITY_EARTH
						* Math.cos(values[2]) * Math.sin(values[1]));

				// Find the gravity component of the Z-axis
				// = -g*cos(roll)*cos(pitch);
				components[2] = (float) (SensorManager.GRAVITY_EARTH
						* Math.cos(values[2]) * Math.cos(values[1]));
			}

			// Subtract the gravity component of the signal
			// from the input acceleration signal to get the
			// tilt compensated output.
			linearAcceleration[0] = (this.acceleration[0] - components[0])
					/ SensorManager.GRAVITY_EARTH;
			linearAcceleration[1] = (this.acceleration[1] - components[1])
					/ SensorManager.GRAVITY_EARTH;
			linearAcceleration[2] = (this.acceleration[2] - components[2])
					/ SensorManager.GRAVITY_EARTH;
		}

		return linearAcceleration;
	}

	public void setLpfAccelerationActive(boolean lpfAccelerationActive)
	{
		this.lpfAccelerationActive = lpfAccelerationActive;
	}

	public void setLpfMagneticActive(boolean lpfMagneticActive)
	{
		this.lpfMagneticActive = lpfMagneticActive;
	}

	public void setMeanFilterAccelerationActive(
			boolean meanFilterAccelerationActive)
	{
		this.meanFilterAccelerationActive = meanFilterAccelerationActive;
	}

	public void setMeanFilterMagneticActive(boolean meanFilterMagneticActive)
	{
		this.meanFilterMagneticActive = meanFilterMagneticActive;
	}

	/**
	 * Read in the current user preferences.
	 */
	private void readPrefs()
	{
		SharedPreferences prefs = this.context.getSharedPreferences(
				"filter_prefs", Activity.MODE_PRIVATE);

		this.lpfAccelerationActive = prefs
				.getBoolean("lpf_acceleration", false);
		this.lpfMagneticActive = prefs.getBoolean("lpf_magnetic", false);

		this.meanFilterAccelerationActive = prefs.getBoolean(
				"mean_filter_acceleration", false);
		this.meanFilterMagneticActive = prefs.getBoolean(
				"mean_filter_magnetic", false);

	}
}
