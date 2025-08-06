package com.cehpoint.netwin.presentation.events

import com.cehpoint.netwin.domain.model.RegistrationStepData

sealed class RegistrationFlowEvent {
    data class UpdateData(val transform: RegistrationStepData.() -> RegistrationStepData) : RegistrationFlowEvent()
    object Next : RegistrationFlowEvent()
    object Previous : RegistrationFlowEvent()
    object Reset : RegistrationFlowEvent()
    object Submit : RegistrationFlowEvent()
}
