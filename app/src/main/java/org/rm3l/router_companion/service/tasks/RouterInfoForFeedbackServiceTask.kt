package org.rm3l.router_companion.service.tasks

import android.content.Context
import org.rm3l.router_companion.resources.conn.NVRAMInfo
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.tiles.status.router.StatusRouterCPUTile
import org.rm3l.router_companion.utils.SSHUtils
import org.rm3l.router_companion.utils.Utils

class RouterInfoForFeedbackServiceTask(ctx: Context) : AbstractBackgroundServiceTask(ctx) {

    @Throws(Exception::class)
    override fun runBackgroundServiceTask(router: Router) {

        val routerPreferences = mCtx.getSharedPreferences(
            router.templateUuidOrUuid,
            Context.MODE_PRIVATE
        ) ?: return

        val manualProperty = SSHUtils.getManualProperty(
            mCtx, router, globalPreferences, "uname -a",
            StatusRouterCPUTile.GREP_MODEL_PROC_CPUINFO + "| uniq",
            StatusRouterCPUTile.GREP_MODEL_PROC_CPUINFO + "| wc -l", "/sbin/softwarerevision 2>&1",
            "cat /tmp/loginprompt 2>&1"
        ) ?: return

        var kernel: String? = null
        val kernelFromPrefs = routerPreferences.getString(NVRAMInfo.KERNEL, DEFAULT_VALUE)
        if (manualProperty.isNotEmpty()) {
            kernel = manualProperty[0]
        }
        var cpuModel: String? = null
        val cpuModelFromPrefs = routerPreferences.getString(NVRAMInfo.CPU_MODEL, DEFAULT_VALUE)
        if (manualProperty.size >= 2) {
            cpuModel = manualProperty[1]
        }
        var cpuCoresCount: String? = null
        val cpuCoresCountFromPrefs = routerPreferences.getString(
            NVRAMInfo.CPU_CORES_COUNT,
            DEFAULT_VALUE
        )
        if (manualProperty.size >= 3) {
            cpuCoresCount = manualProperty[2]
        }
        var ddwrtRev: String? = null
        val ddwrtRevFromPrefs = routerPreferences.getString(NVRAMInfo.REVISION, DEFAULT_VALUE)
        if (manualProperty.size >= 4) {
            ddwrtRev = manualProperty[3]
        }
        var loginPrompt = ""
        val loginPromptFromPrefs = routerPreferences.getString(NVRAMInfo.LOGIN_PROMPT, DEFAULT_VALUE)
        if (manualProperty.size >= 5) {
            for (i in 3..manualProperty.size - 1) {
                loginPrompt += manualProperty[i] + "\n"
            }
        }

        val editor = routerPreferences.edit()
        var prefsEdited = false
        if (!(DEFAULT_VALUE == kernel || kernelFromPrefs == kernel)) {
            editor.putString(NVRAMInfo.KERNEL, kernel)
            prefsEdited = true
        }
        if (!(DEFAULT_VALUE == cpuModel || cpuModelFromPrefs == cpuModel)) {
            editor.putString(NVRAMInfo.CPU_MODEL, cpuModel)
            prefsEdited = true
        }
        if (!(DEFAULT_VALUE == cpuCoresCount || cpuCoresCountFromPrefs == cpuCoresCount)) {
            editor.putString(NVRAMInfo.CPU_CORES_COUNT, cpuCoresCount)
            prefsEdited = true
        }
        if (!(DEFAULT_VALUE == ddwrtRev || ddwrtRevFromPrefs == ddwrtRev)) {
            editor.putString(NVRAMInfo.REVISION, ddwrtRev)
            prefsEdited = true
        }
        if (!(DEFAULT_VALUE == loginPrompt || loginPromptFromPrefs == loginPrompt)) {
            editor.putString(NVRAMInfo.LOGIN_PROMPT, loginPrompt)
            prefsEdited = true
        }

        if (prefsEdited) {
            editor.apply()
            Utils.requestBackup(mCtx)
        }
    }

    companion object {
        val DEFAULT_VALUE = "-"
    }
}
