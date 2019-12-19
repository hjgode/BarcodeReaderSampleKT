package com.learn

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.honeywell.aidc.*
import com.honeywell.aidc.BarcodeReader
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity(),BarcodeReader.BarcodeListener, AidcManager.CreatedCallback,BarcodeReader.TriggerListener {

    private var aidcManager: AidcManager? = null
    private var barcodeReader: BarcodeReader? = null
    private var triggerState = false
    val TAG: String="BarcodeReaderKT"
    var savedProperties: Map<String, Any>? = null
    /*
    DIFF between ? and !!
    +------------+--------------------+---------------------+----------------------+
    | a: String? |           a.length |           a?.length |           a!!.length |
    +------------+--------------------+---------------------+----------------------+
    |      "cat" | Compile time error |                   3 |                    3 |
    |       null | Compile time error |                null | NullPointerException |
    +------------+--------------------+---------------------+----------------------+
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AidcManager.create(applicationContext, this)

        //no need to define btn first, as we imported import kotlinx.android.synthetic.main.activity_main.*
        //no new OnClickListener, as there is only one setOnClickListener
        btn.setOnClickListener {
            barcodeReader?.softwareTrigger(true)
        }

        btnProp.setOnClickListener {
            barcodeReader?.startPropertyEditor(this);
        }
    }

    override fun onCreated(manager: AidcManager?)//fun = funcation
    {
        aidcManager = manager

        barcodeReader = manager!!.createBarcodeReader()
        if(barcodeReader != null)
        {
            barcodeReader!!.claim() //!! = I know it might be null but trust me

            //save current properties for later restore
            savedProperties = barcodeReader!!.getAllProperties()

            if(barcodeReader!!.loadProfile("nowedge"))
                Log.d(TAG, "nowedge profile loaded")
            else
                Log.d(TAG, "nowedge profile load FAILED")

            barcodeReader!!.addBarcodeListener(this)
            barcodeReader!!.addTriggerListener(this);

            val properties: MutableMap<String, Any> =
                HashMap()
            // Set Symbologies On/Off
            properties[BarcodeReader.PROPERTY_CODE_128_ENABLED] = true
            properties[BarcodeReader.PROPERTY_GS1_128_ENABLED] = true
            properties[BarcodeReader.PROPERTY_QR_CODE_ENABLED] = true
            properties[BarcodeReader.PROPERTY_CODE_39_ENABLED] = true
            properties[BarcodeReader.PROPERTY_DATAMATRIX_ENABLED] = true
            properties[BarcodeReader.PROPERTY_UPC_A_ENABLE] = true
            properties[BarcodeReader.PROPERTY_EAN_13_ENABLED] = false
            properties[BarcodeReader.PROPERTY_AZTEC_ENABLED] = false
            properties[BarcodeReader.PROPERTY_CODABAR_ENABLED] = false
            properties[BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED] = true
            properties[BarcodeReader.PROPERTY_PDF_417_ENABLED] = false
            // Set Max Code 39 barcode length
            properties[BarcodeReader.PROPERTY_CODE_39_MAXIMUM_LENGTH] = 17
            // Turn on center decoding
            properties[BarcodeReader.PROPERTY_CENTER_DECODE] = true
            // Disable bad read response, handle in onFailureEvent
            properties[BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED] = false

            properties["DPR_WEDGE"] = false //NO EFFECT as long as app is running, but then
            properties["TRIG_ENABLE"] = false //NO EFFECT as long as app is running, but then

            // Apply the settings
            barcodeReader?.setProperties(
                properties
            )

            barcodeReader!!.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE, BarcodeReader.TRIGGER_CONTROL_MODE_CLIENT_CONTROL);
        }
    }

    override fun onTriggerEvent(p0: TriggerStateChangeEvent?)
    {
        p0?.state?.let { barcodeReader?.aim(it) }   //do a 'safe' call
        barcodeReader!!.light(p0!!.state)
        barcodeReader!!.decode(p0.state)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        if (barcodeReader != null) {
            try {
                barcodeReader!!.claim()
                /*
            // register trigger state change listener
            barcodeReader.addTriggerListener(this);
            */

            } catch (e: ScannerUnavailableException) {
                e.printStackTrace()
                Toast.makeText(this, "Scanner unavailable", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        if (barcodeReader != null) {
            try {
                barcodeReader!!.release()
            } catch (e: ScannerUnavailableException) {
                e.printStackTrace()
                Toast.makeText(this, "Scanner unavailable", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        if (barcodeReader != null) {
            // unregister barcode event listener
            barcodeReader!!.removeBarcodeListener(this);

            // unregister trigger state change listener
            barcodeReader!!.removeTriggerListener(this);

            //restore saved settings
            if(barcodeReader!!.loadProfile("Default"))
                Log.d(TAG, "Default profile loaded")
            else
                Log.d(TAG, "Default profile load FAILED")

            barcodeReader!!.setProperties(savedProperties)

            // close BarcodeReader to clean up resources.
            // once closed, the object can no longer be used.
            barcodeReader!!.close();
        }
        if (aidcManager != null) {
            // close AidcManager to disconnect from the scanner service.
            // once closed, the object can no longer be used.
            aidcManager!!.close();
        }

    }

    override fun onBarcodeEvent(p0: BarcodeReadEvent?) {
        Log.d(TAG, "onBarcodeReadEvent")
        runOnUiThread {
            val barcodeData: String = p0!!.getBarcodeData()
            val timestamp: String = p0.getTimestamp()
            // update UI to reflect the data
            edit.setText(timestamp + "\n" + barcodeData)
        }
    }

    override fun onFailureEvent(p0: BarcodeFailureEvent?) {
        Log.d(TAG, "onFailureEvent")
        runOnUiThread {
            Toast.makeText(
                this@MainActivity, "Barcode read failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}
