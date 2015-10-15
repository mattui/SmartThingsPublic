definition(
        name: "Programmable Thermostat",
        namespace: "mattui",
        author: "Matt Klepac",
        description: "Weekday and Weekend Thermostat",
        category: "Green Living",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@3x.png")

preferences {
    section("Choose thermostat (s)") {
        input "thermostats", "capability.thermostat", required: true, multiple:true
    }

    section("Monday to Friday Schedule") {
        input "weekdayWakeTime", "time", title: "Wake Time", required: true
            input "weekdayWakeHeat", "number", title: "Wake Heat Temp", required: true
            input "weekdayWakeCool", "number", title: "Wake Cool Temp", required: true
            input "weekdayLeaveTime", "time", title: "Leave Time", required: true
            input "weekdayLeaveHeat", "number", title: "Leave Heat Temp", required: true
            input "weekdayLeaveCool", "number", title: "Leave Cool Temp", required: true
            input "weekdayReturnTime", "time", title: "Return Time", required: true
            input "weekdayReturnHeat", "number", title: "Return Heat Temp", required: true
            input "weekdayReturnCool", "number", title: "Return Cool Temp", required: true
            input "weekdaySleepTime", "time", title: "Sleep Time", required: true
            input "weekdaySleepHeat", "number", title: "Sleep Heat Temp", required: true
            input "weekdaySleepCool", "number", title: "Sleep Cool Temp", required: true
    }
    section("Saturday and Sunday Schedule") {
        input "weekendWakeTime", "time", title: "Wake Time", required: true
            input "weekendWakeHeat", "number", title: "Wake Heat Temp", required: true
            input "weekendWakeCool", "number", title: "Wake Cool Temp", required: true
            input "weekendLeaveTime", "time", title: "Leave Time", required: true
            input "weekendLeaveHeat", "number", title: "Leave Heat Temp", required: true
            input "weekendLeaveCool", "number", title: "Leave Cool Temp", required: true
            input "weekendReturnTime", "time", title: "Return Time", required: true
            input "weekendReturnHeat", "number", title: "Return Heat Temp", required: true
            input "weekendReturnCool", "number", title: "Return Cool Temp", required: true
            input "weekendSleepTime", "time", title: "Sleep Time", required: true
            input "weekendSleepHeat", "number", title: "Sleep Heat Temp", required: true
            input "weekendSleepCool", "number", title: "Sleep Cool Temp", required: true
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
        initialize();
}

def updated(settings) {
    unschedule()
        initialize()
}

def initialize() {
    schedule(weekdayWakeTime, "weekdayWakeTimeCallback")
        schedule(weekdayLeaveTime, "weekdayLeaveTimeCallback")
        schedule(weekdayReturnTime, "weekdayReturnTimeCallback")
        schedule(weekdaySleepTime, "weekdaySleepTimeCallback")
        schedule(weekendWakeTime, "weekendWakeTimeCallback")
        schedule(weekendLeaveTime, "weekendLeaveTimeCallback")
        schedule(weekendReturnTime, "weekendReturnTimeCallback")
        schedule(weekendSleepTime, "weekendSleepTimeCallback")
}

def setTemp(heat, cool) {
    thermostats.each { thermostat ->
        def thermostatState = thermostat.currentThermostatMode
            log.debug "Thermostat mode = $thermostatState"
            def thermostatFan = thermostat.currentThermostatFanMode
            log.debug "Thermostat fan = $thermostatFan"
            if (thermostatState == "auto") {
                thermostat.setHeatingSetpoint(heat)
                    thermostat.setCoolingSetpoint(cool)
                    log.info "Set $thermostat Heat $heat°, Cool $cool°"
            }
            else if (thermostatState == "heat") {
                thermostat.setHeatingSetpoint(heat)
                    log.info "Set $thermostat Heat $heat°"
            }
            else {
                thermostat.setCoolingSetpoint(cool)
                    log.info "Set $thermostat Cool $cool°"
            }
    }
}

def isWeekday() {
    def calendar = Calendar.getInstance()
        def dow = calendar.get(Calendar.DAY_OF_WEEK)
        return ((dow >= Calendar.MONDAY) && (dow <= Calendar.FRIDAY))
}

def weekdayWakeTimeCallback() {
    if (isWeekday()) {
        setTemp(weekdayWakeHeat, weekdayWakeCool)
    }
}

def weekdayLeaveTimeCallback() {
    if (isWeekday()) {
        setTemp(weekdayLeaveHeat, weekdayLeaveCool)
    }
}

def weekdayReturnTimeCallback() {
    if (isWeekday()) {
        setTemp(weekdayReturnHeat, weekdayReturnCool)
    }
}

def weekdaySleepTimeCallback() {
    if (isWeekday()) {
        setTemp(weekdaySleepHeat, weekdaySleepCool)
    }
}

def weekendWakeTimeCallback() {
    if (!isWeekday()) {
        setTemp(weekendWakeHeat, weekendWakeCool)
    }
}

def weekendLeaveTimeCallback() {
    if (!isWeekday()) {
        setTemp(weekendLeaveHeat, weekendLeaveCool)
    }
}

def weekendReturnTimeCallback() {
    if (!isWeekday()) {
        setTemp(weekendReturnHeat, weekendReturnCool)
    }
}

def weekendSleepTimeCallback() {
    if (!isWeekday()) {
        setTemp(weekendSleepHeat, weekendSleepCool)
    }
}
