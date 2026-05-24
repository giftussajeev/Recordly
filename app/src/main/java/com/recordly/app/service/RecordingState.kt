package com.recordly.app.service

sealed class RecordingState {
    object Idle : RecordingState()
    object RequestingPermission : RecordingState()
    data class Countdown(val secondsLeft: Int) : RecordingState()
    object Recording : RecordingState()
    object Paused : RecordingState()
    object Stopping : RecordingState()
    object Saved : RecordingState()
    data class Error(val message: String) : RecordingState()
}
