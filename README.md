CardanLinearAcceleration
========================

Calcuations Linear Acceleration on Android devices using Cardan angles and a acceleration and magnetic sensor fusion.

1. Linear Acceleration:

An acceleromter can measure the static gravitation field of earth (like a tilt sensor) or it can measure measure linear acceleration (like accelerating in a vehicle), but it cannot measure both at the same time. When talking about linear acceleration in reference to an acceleration sensor, what we really mean is Linear Acceleration = Measured Acceleration - Gravity. The hard part is determining what part of the signal is gravity.

2. The Problem:

It is difficult to sequester the gravity component of the signal from the linear acceleration. Some Android devices implement Sensor.TYPE_LINEAR_ACCELERATION and Sensor.TYPE_GRAVITY which perform the calculations for you. Most of these devices are new and equipped with a gyroscope. If you have and older device and do not have a gyroscope, you are going to face some limitations with Sensor.TYPE_ACCELERATION. The tilt of the device can only be measured accurately assuming the device is not experiencing any linear acceleration. The linear acceleration can only be measured accurately if the tilt of the device is known.

3. Sensor Fusions:

Sensor fusions take measurements from multiple sensors and fuse them together to create a better estimation than either sensor could do by itself. The most common type of sensor fusion to determine linear acceleration is an accelerometer and gyroscope, which measures the rotation of the device. If you know the rotation of the device and the acceleration of gravity, you can determine the tilt of the device and subtract that from the measured acceleration to get linear acceleration. However, not all devices have gyroscopes.

3.2. What are the advantages?

The biggest advantage to using Cardan angles instead of a low-pass filter to determine the linear acceleration of a device is that the Cardan angles are extremely responsive to changes in rotation. That is the only advantage, but it is a really big one. It is slightly more difficult to implement a low-pass filter that is responsive to tilt changes and sensitive to linear acceleration at the same time.

4. The Algorithm Overview:

 The algorithm follows a simple process:

1.) Determine the rotation matrix that puts the devices sensors into the world-coordinate system
2.) Determine the Cardan angles( in the world-coordinate system) of the device using the accelerometer and magnetometer sensors
3.) Use the rotation matrix to put the accelerometer measurements into the world-coordinate system and determine the gravity of earth as measured by the device
4.) Determine if the device is experiencing linear acceleration
5.) If no linear acceleration is detected, use some trigonometry with the Cardan angles and the measured gravity of earth to determine the gravity component of the acceleration
6.) If linear acceleration is detected, use the last known good estimation of the tilt
7.) Subtract the gravity component from the acceleration to determine the linear acceleration

![Alt text](http://blog.kircherelectronics.com/blog/images/nexus_4_linear_acceleartion.png "Cardan Linear Acceleration")
