package com.magic_ar.slamar_android;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.location.LocationManager;

/**
 * Created by cgnerds on 2017/8/21.
 */

public class IMU implements SensorEventListener{
    private long timestampAccelerometerSensor, timestampLinearAccelerationSensor, timestampRotationSensor;
    private long timestampGravitySensor, timestampGyroscopeSensor, timestampMagneticFieldSensor;
    private String tAccelerometerSensor, tLinearAccelerationSensor, rRotationSensor;
    private String tGravitySensor, tGyroscopeSensor, tMagneticFieldSensor, tGPS;
    private long countAccelerometerSensor, countLinearAccelerationSensor, countRotationSensor;
    private long countGravitySensor, countGyroscopeSensor, countMagneticFieldSensor;

    // for GPS
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double longitude, latitude;

    // fro IMU
    private SensorManager mSensorManager;
    private Sensor accelerometerSensor;
    private Sensor linearAccelerationSensor;
    private Sensor rotationSensor;
    private Sensor gravititySensor;
    private Sensor gyroscopeSensor;

    private Sensor magneticFieldSensor;

    float accelerationForce[] = new float[3];       // Including gravity, acceleration force along x, y, z axis in m/s²
    float linearAccelerationForce[] = new float[3]; // Excluding gravity, acceleration force along x, y, z axis in m/s²
    float gravity[] = new float[3];                 // Force of gravity along x, y, z axis in m/s²
    float gyroscope[] = new float[3];               // Rate of rotation around x, y, z axis in rad/s
    float rotation[] = new float[4];                // Rotation vector component along the x, y, z axis and scalar component of the rotation vector
    float magneticField[] = new float[3];           // Geomagnetic filed strength along x, y, z axis

    public void create()
    {
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // Change the cursor.

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
