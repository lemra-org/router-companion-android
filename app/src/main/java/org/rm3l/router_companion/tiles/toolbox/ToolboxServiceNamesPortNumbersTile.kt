/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */
package org.rm3l.router_companion.tiles.toolbox

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.actions.AbstractRouterAction
import org.rm3l.router_companion.actions.ServiceNamesPortNumbersMappingLookupAction
import org.rm3l.router_companion.api.iana.Protocol
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.utils.kotlin.gone
import org.rm3l.router_companion.utils.kotlin.visible

class ToolboxServiceNamesPortNumbersTile(parentFragment: Fragment, arguments: Bundle?, router: Router?) :
    AbstractToolboxTile(parentFragment, arguments, router) {

    private val transportProtocolValuesFromSpinner: Array<String>
    private val portInputLayout: TextInputLayout
    private val serviceInputLayout: TextInputLayout

    init {
        // We will use a custom layout for this
        layout.findViewById<View>(R.id.tile_toolbox_abstract_edittext).gone()
        val lookupView = layout.findViewById<View>(R.id.tile_toolbox_abstract_service_port_lookup_layout)
        lookupView.visible()
        portInputLayout = lookupView.findViewById(R.id.tile_toolbox_abstract_service_port_lookup_port_textinputlayout)
        serviceInputLayout = lookupView.findViewById(R.id.tile_toolbox_abstract_service_port_lookup_service_textinputlayout)
        lookupView.findViewById<RadioGroup>(R.id.tile_toolbox_abstract_service_port_lookup_type)
            .setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.tile_toolbox_abstract_service_port_lookup_port -> {
                        portInputLayout.visible()
                        serviceInputLayout.gone()
                    }
                    R.id.tile_toolbox_abstract_service_port_lookup_service -> {
                        portInputLayout.gone()
                        serviceInputLayout.visible()
                    }
                }
            }
        // Default is to lookup by port
        lookupView.findViewById<RadioButton>(R.id.tile_toolbox_abstract_service_port_lookup_port).isChecked = true

        transportProtocolValuesFromSpinner = mParentFragmentActivity.resources
            .getStringArray(R.array.service_port_lookup_protocol_array_values)
    }

    override fun isGeoLocateButtonEnabled() = false
    override fun getEditTextHint() = null
    override fun getInfoText() = R.string.service_port_lookup_info

    override fun checkInputAnReturnErrorMessage(inputText: String): CharSequence? {
        // Main input is hidden - proceed with custom fields instead
        val port = layout.findViewById<TextInputEditText>(R.id.tile_toolbox_abstract_service_port_lookup_port_edittext).text
        val serviceName = layout.findViewById<TextInputEditText>(R.id.tile_toolbox_abstract_service_port_lookup_service_edittext).text
        return when {
            layout.findViewById<RadioButton>(R.id.tile_toolbox_abstract_service_port_lookup_port).isChecked -> {
                val errorMsg: CharSequence? = when {
                    TextUtils.isEmpty(port) -> "Empty Port Number"
                    port.toString().toLongOrNull() == null -> "Invalid Port Number: must be a number"
                    else -> null
                }
                if (errorMsg.isNullOrBlank()) {
                    portInputLayout.isErrorEnabled = false
                    portInputLayout.error = null
                } else {
                    portInputLayout.isErrorEnabled = true
                    portInputLayout.error = errorMsg
                }
                errorMsg
            }
            layout.findViewById<RadioButton>(R.id.tile_toolbox_abstract_service_port_lookup_service).isChecked -> {
                val errorMsg: CharSequence? = if (TextUtils.isEmpty(serviceName)) "Empty Service Name" else null
                if (errorMsg.isNullOrBlank()) {
                    serviceInputLayout.isErrorEnabled = false
                    serviceInputLayout.error = null
                } else {
                    serviceInputLayout.isErrorEnabled = true
                    serviceInputLayout.error = errorMsg
                }
                errorMsg
            }
            else -> null
        }
    }

    override fun getRouterAction(textToFind: String): AbstractRouterAction<*>? {
        val port = layout.findViewById<TextInputEditText>(R.id.tile_toolbox_abstract_service_port_lookup_port_edittext).text
        val serviceName = layout.findViewById<TextInputEditText>(R.id.tile_toolbox_abstract_service_port_lookup_service_edittext).text
        val protocolSpinner = layout.findViewById<Spinner>(R.id.tile_toolbox_abstract_service_port_lookup_protocol)
        var ports: Collection<Long>? = null
        var serviceNames: Collection<String>? = null
        if (layout.findViewById<RadioButton>(R.id.tile_toolbox_abstract_service_port_lookup_port).isChecked) {
            try {
                ports = listOf(port.toString().toLong())
            } catch (e: Exception) {
                Toast.makeText(mParentFragmentActivity, "Invalid input: $port", Toast.LENGTH_SHORT).show()
                return null
            }
        } else if (layout.findViewById<RadioButton>(R.id.tile_toolbox_abstract_service_port_lookup_service).isChecked) {
            serviceNames = listOf(serviceName.toString())
        }
        return try {
            ServiceNamesPortNumbersMappingLookupAction(
                router = mRouter!!,
                listener = mRouterActionListener,
                globalSharedPreferences = mGlobalPreferences,
                protocols = listOf(Protocol.valueOf(transportProtocolValuesFromSpinner[protocolSpinner.selectedItemPosition])),
                ports = ports,
                services = serviceNames
            )
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Toast.makeText(mParentFragmentActivity, "Internal Error: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    override fun getSubmitButtonText() = R.string.toolbox_service_port_lookup_submit
    override fun getTileTitle() = R.string.service_port_lookup
}
