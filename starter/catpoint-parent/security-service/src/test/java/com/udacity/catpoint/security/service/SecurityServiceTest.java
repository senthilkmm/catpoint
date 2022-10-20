package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    private SecurityService securityService;
    private Sensor doorSensor;
    private Sensor windowSensor;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private  ImageService imageService;

    @Mock
    private BufferedImage bufferedImage;

    @BeforeEach
    public void init() {
        securityService = new SecurityService(securityRepository, imageService);

        doorSensor = new Sensor("Door Sensor", SensorType.DOOR);
        windowSensor = new Sensor("Window Sensor", SensorType.WINDOW);
    }

//    1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @Test
    public void armed_sensorActivated_setToPendingAlarm() {
        // current arming status to be armed home, and alarm status to be no alarm
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        // activate the sensor
        securityService.changeSensorActivationStatus(doorSensor,true);

        // get alarm status to be called twice
        verify(securityRepository, times(2)).getAlarmStatus();
        // alarm status to be set to pending alarm but never to alarm or no alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//    2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @Test
    public void armed_sensorActivated_inPendingAlarm_setToAlarm() {
        // current arming status to be armed home, and alarm status to be pending alarm
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // activate the sensor
        securityService.changeSensorActivationStatus(doorSensor, true);

        // get alarm status to be called twice
        verify(securityRepository, times(2)).getAlarmStatus();
        // alarm status to be set to alarm but never to pending alarm or no alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//    3. If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    public void pendingAlarm_allSensorsInactive_setToNoAlarmState() {
        // activate all sensors, current alarm status to be pending alarm
        doorSensor.setActive(true);
        windowSensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // deactivate door sensor
        securityService.changeSensorActivationStatus(doorSensor, false);

        // deactivate window sensor
        securityService.changeSensorActivationStatus(windowSensor, false);

        // get alarm status to be called 4 times
        verify(securityRepository, times(4)).getAlarmStatus();
        // alarm to status to be set to no alarm twice
        verify(securityRepository, times(2)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//    4. If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    public void alarmActive_changeSensorState_alarmStateNotAffected() {
        // activate door sensor, alarm status to be alarm
        doorSensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // deactivate door sensor
        securityService.changeSensorActivationStatus(doorSensor, false);

        // get alarm status to be called once
        verify(securityRepository, times(1)).getAlarmStatus();
        // alarm status should not change
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

//    5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    public void activateAlreadyActiveSensor_inPendingAlarm_setToAlarm() {
        // activate door sensor, alarm status to be pending alarm
        doorSensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // re-activate door sensor
        securityService.changeSensorActivationStatus(doorSensor, true);

        // get alarm status to be called once
        verify(securityRepository, times(1)).getAlarmStatus();
        // get arming status should not be called
        verify(securityRepository, never()).getArmingStatus();
        // alarm status should not be affected
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }

//    6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    public void deactivateAlreadyInactiveSensor_alarmStateNotAffected(AlarmStatus alarmStatus) {
        // set alarm status
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);

        // by default the sensor is inactive, deactivate door sensor
        securityService.changeSensorActivationStatus(doorSensor, false);

        // get alarm status should be called once
        verify(securityRepository, times(1)).getAlarmStatus();
        // get arming status should not be called
        verify(securityRepository, never()).getArmingStatus();
        // alarm status should never change
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

//    7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    @Test
    public void armedHome_catDetected_setToAlarm() {
        // set armed status to armed_home, make cat detected true
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        // process the image
        securityService.processImage(bufferedImage);

        // image service should be called once
        verify(imageService,times(1)).imageContainsCat(any(BufferedImage.class), anyFloat());
        // get arming status should be called once
        verify(securityRepository, times(1)).getArmingStatus();
        // alarm status should be set to alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

//    8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    public void catNotDetected_sensorsNotActive_setToNoAlarm() {
        // make cat detected true
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);

        // process the image
        securityService.processImage(bufferedImage);

        // image service should be called once
        verify(imageService,times(1)).imageContainsCat(any(BufferedImage.class), anyFloat());
        // get arming status should never be called
        verify(securityRepository, never()).getArmingStatus();
        // get sensors should be called once to ensure no active alarm
        verify(securityRepository, times(1)).getSensors();
        // alarm status should be set to no alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void catNotDetected_atLeastOneSensorActive_alarmStateNotAffected() {
        // make cat detected true, activate one of the sensors
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        doorSensor.setActive(false);
        windowSensor.setActive(true);
        when(securityRepository.getSensors()).thenReturn(Set.of(doorSensor, windowSensor));

        // process the image
        securityService.processImage(bufferedImage);

        // image service should be called once
        verify(imageService,times(1)).imageContainsCat(any(BufferedImage.class), anyFloat());
        // get arming status should never be called
        verify(securityRepository, never()).getArmingStatus();
        // get sensors should be called once to ensure no active alarm
        verify(securityRepository, times(1)).getSensors();
        // alarm status should be set to no alarm
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

//    9. If the system is disarmed, set the status to no alarm.
    @Test
    public void disarm_setToNoAlarm() {
        // set to disarmed
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        // arming status to be set to disarmed
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.DISARMED);
        // get arming status never to be called
        verify(securityRepository, never()).getArmingStatus();
        // get alarm status never to be called
        verify(securityRepository, never()).getAlarmStatus();
        // update sensor never to be called
        verify(securityRepository, never()).updateSensor(any(Sensor.class));
        // alarm status to be set to no alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//    10. If the system is armed, reset all sensors to inactive.
    @Test
    public void setArmedHome_setAllSensorsToInactive() {
        // current arming status: disarmed, all sensors active
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getSensors()).thenReturn(Set.of(doorSensor, windowSensor));
        securityRepository.getSensors().forEach(sensor -> sensor.setActive(true));

        // set arming status to armed home
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // arming status to be set to armed home
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_HOME);
        // update sensor called for all sensors
        verify(securityRepository, times(securityRepository.getSensors().size())).updateSensor(any(Sensor.class));
        // all sensors to be inactive
        assertFalse(securityRepository.getSensors().stream().anyMatch(Sensor::getActive));
    }

    @Test
    public void setArmedAway_setAllSensorsToInactive() {
        // current arming status: disarmed, all sensors active
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getSensors()).thenReturn(Set.of(doorSensor, windowSensor));
        securityRepository.getSensors().forEach(sensor -> sensor.setActive(true));

        // set arming status to armed away
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        // arming status to be set to armed home
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_AWAY);
        // update sensor called for all sensors
        verify(securityRepository, times(securityRepository.getSensors().size())).updateSensor(any(Sensor.class));
        // all sensors to be inactive
        assertFalse(securityRepository.getSensors().stream().anyMatch(Sensor::getActive));
    }

    @Test
    public void setArmedAway_oneSensorActive_setAllSensorsToInactive() {
        // current arming status: disarmed, door sensor active, window sensor inactive
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getSensors()).thenReturn(Set.of(doorSensor, windowSensor));
        doorSensor.setActive(false);
        windowSensor.setActive(true);

        // set arming status to armed away
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        // arming status to be set to armed home
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_AWAY);
        // update sensor called for all sensors
        verify(securityRepository, times(securityRepository.getSensors().size())).updateSensor(any(Sensor.class));
        // all sensors to be inactive
        assertFalse(securityRepository.getSensors().stream().anyMatch(Sensor::getActive));
    }

//    11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    public void setArmedHome_catDetected_setToAlarm() {
        // current arming status: disarmed, camera shows cat
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);

        // set arming status to armed home
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // arming status to be set to armed home
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_HOME);
        // to be set to alarm state
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void setArmedAway_catDetected_alarmNotAffected() {
        // current arming status: disarmed, camera shows cat
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);

        // set arming status to armed away
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        // arming status to be set to armed home
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_AWAY);
        // alarm state should not change
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void setArmedHomeFromAway_catDetected_setToAlarm() {
        // current arming status: armed away, camera shows cat
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);

        // set arming status to armed home
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // arming status to be set to armed home
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_HOME);
        // to be set to alarm state
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
}