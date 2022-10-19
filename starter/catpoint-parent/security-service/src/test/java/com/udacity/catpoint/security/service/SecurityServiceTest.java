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
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(doorSensor,true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

//    2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @Test
    public void armed_sensorActivated_inPendingAlarm_setToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(doorSensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

//    3. If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    public void pendingAlarm_allSensorsInactive_setToNoAlarmState() {
        doorSensor.setActive(true);
        windowSensor.setActive(true);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(doorSensor, false);
        securityService.changeSensorActivationStatus(windowSensor, false);

        verify(securityRepository, times(2)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//    4. If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    public void alarmActive_changeSensorState_alarmStateNotAffected() {
        doorSensor.setActive(true);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(doorSensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

//    5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    public void activateAlreadyActiveSensor_inPendingAlarm_setToAlarm() {
        doorSensor.setActive(true);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(doorSensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

//    6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    public void deactivateAlreadyInactiveSensor_alarmStateNotAffected(AlarmStatus alarmStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);

        securityService.changeSensorActivationStatus(doorSensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

//    7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    @Test
    public void armedHome_catDetected_setToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        securityService.processImage(bufferedImage);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

//    8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    public void catNotDetected_sensorsNotActive_setToNoAlarm() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);

        securityService.processImage(bufferedImage);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//    9. If the system is disarmed, set the status to no alarm.
    @Test
    public void disArm_setToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//    10. If the system is armed, reset all sensors to inactive.
    @Test
    public void arm_setAllSensorsToInactive() {
        when(securityRepository.getSensors()).thenReturn(Set.of(doorSensor, windowSensor));
        securityRepository.getSensors().forEach(sensor -> sensor.setActive(true));

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        assertFalse(securityRepository.getSensors().stream().anyMatch(Sensor::getActive));
    }
//    11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    public void arm_catDetected_setToAlarm() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
}