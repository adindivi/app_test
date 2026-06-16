package com.example.domain

// Data model representing a break period
data class BreakPeriod(
    val id: Int,
    val start: Int, // in minutes from midnight (e.g. 06:45 is 405)
    val end: Int,   // in minutes from midnight
    val label: String, // format "HH:MM ~ HH:MM"
    val resume: String // HH:MM when work resumes
) {
    val duration: Int get() = end - start + 1
}

// Data model representing the warning or errors during calculations
data class WarningItem(
    val type: String, // "warn" or "error"
    val msg: String
)

// Main result payload of the calculation
data class CalculationResult(
    val error: Boolean = false,
    val resultMin: Int = 0,
    val resultTimeStr: String = "--:--",
    val pureWorkTime: Int = 0,
    val totalBreakTime: Int = 0,
    val totalElapsed: Int = 0,
    val passedBreaks: List<BreakPeriod> = emptyList(),
    val warnings: List<WarningItem> = emptyList()
)

object ProcessCalculator {
    const val WORK_START = 405  // 06:45 in minutes
    const val WORK_END = 1460   // 24:20 in minutes

    // Static breaks list as defined in the source
    val BREAKS = listOf(
        BreakPeriod(1, 530, 539, "08:50 ~ 08:59", "09:00"),
        BreakPeriod(2, 660, 699, "11:00 ~ 11:39", "11:40"),
        BreakPeriod(3, 820, 829, "13:40 ~ 13:49", "13:50"),
        BreakPeriod(4, 930, 934, "15:30 ~ 15:34", "15:35"),
        BreakPeriod(5, 1060, 1069, "17:40 ~ 17:49", "17:50"),
        BreakPeriod(6, 1190, 1229, "19:50 ~ 20:29", "20:30"),
        BreakPeriod(7, 1350, 1359, "22:30 ~ 22:39", "22:40")
    )

    fun toMinutes(h: Int, m: Int): Int = h * 60 + m

    fun toHHMM(min: Int): String {
        val h = min / 60
        val m = min % 60
        return String.format("%02d:%02d", h, m)
    }

    fun getBreakAt(min: Int): BreakPeriod? {
        return BREAKS.find { min in it.start..it.end }
    }

    fun getNextBreakAfter(min: Int): BreakPeriod? {
        return BREAKS.find { it.start > min }
    }

    fun getPrevBreakBefore(min: Int): BreakPeriod? {
        return BREAKS.findLast { it.end < min }
    }

    /**
     * Calculates the arrival time given a start time and a pure duration in minutes.
     * Takes scheduled breaks and work hours into account.
     */
    fun calcForward(startMin: Int, duration: Int): CalculationResult {
        var current = startMin
        var remaining = duration
        val warnings = mutableListOf<WarningItem>()
        val passedBreaks = mutableListOf<BreakPeriod>()

        if (current < WORK_START) {
            warnings.add(WarningItem("warn", "⚠️ 시업 전(06:45 이전)입니다. 06:45부터 시작합니다."))
            current = WORK_START
        }
        if (current > WORK_END) {
            warnings.add(WarningItem("error", "🚫 종업 시간(24:20) 이후입니다. 입력을 확인해주세요."))
            return CalculationResult(
                error = true,
                resultMin = current,
                resultTimeStr = toHHMM(current),
                pureWorkTime = duration,
                totalBreakTime = 0,
                totalElapsed = 0,
                passedBreaks = emptyList(),
                warnings = warnings
            )
        }

        val onBreak = getBreakAt(current)
        if (onBreak != null) {
            warnings.add(WarningItem("warn", "⚠️ 쉬는 시간(${onBreak.label})입니다. ${onBreak.resume}부터 시작합니다."))
            current = onBreak.end + 1
        }

        while (remaining > 0) {
            val nb = getNextBreakAfter(current - 1)
            if (nb == null || nb.start >= WORK_END) {
                current += remaining
                remaining = 0
            } else {
                val gap = nb.start - current
                if (remaining <= gap) {
                    current += remaining
                    remaining = 0
                } else {
                    remaining -= gap
                    passedBreaks.add(nb)
                    current = nb.end + 1
                }
            }
        }

        if (current > WORK_END) {
            warnings.add(WarningItem("error", "🚫 종업 시간(24:20)을 초과합니다. 익일 이월이 필요합니다."))
        }

        return CalculationResult(
            error = false,
            resultMin = current,
            resultTimeStr = toHHMM(current),
            pureWorkTime = duration,
            totalBreakTime = passedBreaks.sumOf { it.duration },
            totalElapsed = current - startMin,
            passedBreaks = passedBreaks,
            warnings = warnings
        )
    }

    /**
     * Calculates the required start time to finish by endMin with a pure duration in minutes.
     * Takes scheduled breaks and work hours into account.
     */
    fun calcBackward(endMin: Int, duration: Int): CalculationResult {
        var current = endMin
        var remaining = duration
        val warnings = mutableListOf<WarningItem>()
        val passedBreaks = mutableListOf<BreakPeriod>()

        if (current > WORK_END) {
            warnings.add(WarningItem("warn", "⚠️ 종업 시간(24:20) 이후입니다. 24:20 기준으로 역산합니다."))
            current = WORK_END
        }

        val onBreak = getBreakAt(current)
        if (onBreak != null) {
            warnings.add(WarningItem("warn", "⚠️ 쉬는 시간(${onBreak.label})입니다. ${toHHMM(onBreak.start - 1)}을 도착 기준으로 계산합니다."))
            current = onBreak.start - 1
        }

        while (remaining > 0) {
            val pb = getPrevBreakBefore(current)
            if (pb == null || pb.end < WORK_START) {
                current -= remaining
                remaining = 0
            } else {
                val gap = current - (pb.end + 1)
                if (remaining <= gap) {
                    current -= remaining
                    remaining = 0
                } else {
                    remaining -= gap
                    passedBreaks.add(pb)
                    current = pb.start - 1
                }
            }
        }

        if (current < WORK_START) {
            warnings.add(WarningItem("error", "🚫 시업 시간(06:45) 이전으로 역산됩니다. 당일 내 시작 불가."))
        }

        return CalculationResult(
            error = false,
            resultMin = current,
            resultTimeStr = toHHMM(current),
            pureWorkTime = duration,
            totalBreakTime = passedBreaks.sumOf { it.duration },
            totalElapsed = endMin - current,
            passedBreaks = passedBreaks.reversed(),
            warnings = warnings
        )
    }
}
