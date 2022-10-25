package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private final ImageService imageService;
    private final SecurityRepository securityRepository;
    private final Set<StatusListener> statusListeners = new HashSet<>();
    private boolean cat;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus arming status to set
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.DISARMED) {
            securityRepository.setArmingStatus(armingStatus);
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else if (getArmingStatus() == ArmingStatus.DISARMED) {
            securityRepository.setArmingStatus(armingStatus);
            // got armed, set all sensors inactive
            deactivateAllSensors();
            // set to alarm if armed home and cat found
            if (armingStatus == ArmingStatus.ARMED_HOME && cat)
                setAlarmStatus(AlarmStatus.ALARM);
        } else if (getArmingStatus() == ArmingStatus.ARMED_AWAY) {
            // already armed, no need to deactivate sensors
            securityRepository.setArmingStatus(armingStatus);
            // set to alarm if armed home and cat found
            if (armingStatus == ArmingStatus.ARMED_HOME && cat)
                setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    private void deactivateAllSensors() {
        ConcurrentSkipListSet<Sensor> sensors = new ConcurrentSkipListSet<>(getSensors());
        sensors.forEach(sensor -> changeSensorActivationStatus(sensor, false));
        // notify the status listeners of the change in sensor statuses
        statusListeners.forEach(StatusListener::sensorStatusChanged);
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!cat && allSensorsInactive()){
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    private boolean allSensorsInactive() {
        return getSensors().stream().noneMatch(Sensor::getActive);
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status alarm status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        // it won't reach here if in alarm state already
        // at this point only pending alarm should be set to no alarm
        // nothing to do if in no alarm already
        if (securityRepository.getAlarmStatus() == AlarmStatus.PENDING_ALARM) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor sensor object
     * @param active sensor status to set
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        // don't change the alarm sate if already in alarm
        if (getAlarmStatus() != AlarmStatus.ALARM) {
            if (active && getArmingStatus() != ArmingStatus.DISARMED) {
                // from inactive to active or active to active
                handleSensorActivated();
            } else if (sensor.getActive() && !active) {
                // from active to inactive
                handleSensorDeactivated();
            }
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage image to process
     */
    public void processImage(BufferedImage currentCameraImage) {
        // store the cat boolean to use to find already detected cat when moving to armed home
        cat = imageService.imageContainsCat(currentCameraImage, 50.0f);
        catDetected(cat);
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}
