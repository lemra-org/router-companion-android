package org.rm3l.router_companion.job

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator
import com.evernote.android.job.JobRequest
import org.rm3l.router_companion.job.firmware_update.FirmwareUpdateCheckerJob
import org.rm3l.router_companion.job.firmware_update.FirmwareUpdateCheckerOneShotJob
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestAutoRunnerJob
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestRunnerDailyJob
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestRunnerOneShotJob
import org.rm3l.router_companion.job.speedtest.RouterSpeedTestRunnerPeriodicJob
import org.rm3l.router_companion.service.BackgroundService
import org.rm3l.router_companion.service.BackgroundServiceOneShotJob

class RouterCompanionJobCreator : JobCreator {

    override fun create(tag: String): Job? {
        var actualTag: String = tag
        if (tag.startsWith(RouterSpeedTestAutoRunnerJob.JOB_TAG_PREFIX)) {
            // Speed test job have a different format: "$JOB_TAG_PREFIX${TAG}$JOB_TAG_SEPARATOR$routerUuid"
            actualTag = actualTag.substring(RouterSpeedTestAutoRunnerJob.JOB_TAG_PREFIX.length)
                    .split(RouterSpeedTestAutoRunnerJob.JOB_TAG_SEPARATOR)[0]
        }
        val job = JOB_MAP[actualTag]
        return if (job == null) null else job::class.java.newInstance()
    }

    companion object {
        @JvmField
        val JOB_MAP = mapOf<String, Job>(
                FirmwareUpdateCheckerJob.TAG to FirmwareUpdateCheckerJob(),
                FirmwareUpdateCheckerOneShotJob.TAG to FirmwareUpdateCheckerOneShotJob(),
                BackgroundService.TAG to BackgroundService(),
                BackgroundServiceOneShotJob.TAG to BackgroundServiceOneShotJob(),
                RouterSpeedTestRunnerDailyJob.TAG to RouterSpeedTestRunnerDailyJob(),
                RouterSpeedTestRunnerPeriodicJob.TAG to RouterSpeedTestRunnerPeriodicJob(),
                RouterSpeedTestRunnerOneShotJob.TAG to RouterSpeedTestRunnerOneShotJob()
        )

        @JvmStatic
        fun getOneShotJobTags() = JOB_MAP
                .filter { val job = it.value; job is RouterCompanionJob && job.isOneShotJob() }
                .keys
                .toList()

        @JvmStatic
        fun runJobImmediately(tag: String) = JobRequest.Builder(tag).startNow().build().schedule()
    }
}
