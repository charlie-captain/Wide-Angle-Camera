package com.charlie.cameralensinfo

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_camera.*
import java.util.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        title = "Camera Lens Info"
        supportFragmentManager.beginTransaction().add(R.id.content_frame, CameraFragment())
            .commit()
    }
}


class CameraFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val permission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
            return
        } else {
            getCameraLensInfo()
        }

    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menu.add("copy").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.title == "copy") {
            val clipboard: ClipboardManager? = getSystemService(requireContext(), ClipboardManager::class.java)
            val clip = ClipData.newPlainText("camera", getCameraLensInfo())
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "copied", Toast.LENGTH_SHORT).show()
        }

        return true
    }

    private fun getCameraLensInfo(): String {
        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return ""
        val cameraIdList = cameraManager.cameraIdList
        val stringBuilder = StringBuilder()
        stringBuilder.appendln("device: model = ${Build.MODEL}, board = ${Build.BOARD}, device = ${Build.DEVICE}, manufacturer = ${Build.MANUFACTURER}, product = ${Build.PRODUCT}\n")
        stringBuilder.appendln("camera1: " + Camera.getNumberOfCameras())
        stringBuilder.appendln("camera2: " + Arrays.toString(cameraIdList))
        stringBuilder.appendln()

        val frontCameraList = arrayListOf<CameraInformation>()
        val backCameraList = arrayListOf<CameraInformation>()

        cameraIdList.forEach { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            //前置
            val front = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            stringBuilder.appendln("camera id = $id: front = $front")
            //获取相机的物理尺寸
            val size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            stringBuilder.appendln("sensor info : width =  ${size?.width}, height = ${size?.height}")

            val focalLens1 = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)!!
            stringBuilder.appendln("focal lens = ${focalLens1.contentToString()}")
            val w = size!!.width
            val h = size.height
            val horizontalAngle = (2 * Math.atan(w / (focalLens1[0] * 2).toDouble())).toFloat()
            val verticalAngle = (2 * Math.atan(h / (focalLens1[0] * 2).toDouble())).toFloat()
            stringBuilder.appendln("horizontalAngle = $horizontalAngle")
            stringBuilder.appendln("verticalAngle = $verticalAngle")

            stringBuilder.appendln()
            val cameraInfo = CameraInformation(
                cameraId = id,
                isFrontFacing = front,
                sensorSize = size,
                fovHorizontal = horizontalAngle,
                fovVertical = verticalAngle
            )
            if (front) {
                frontCameraList.add(cameraInfo)
            } else {
                backCameraList.add(cameraInfo)
            }
        }

        //简单判断哪个是广角镜头
        //通过FOV(field of view)排序, 从小到大, 角度越大, 越是广角
        val comparator = object : Comparator<CameraInformation> {
            override fun compare(o1: CameraInformation, o2: CameraInformation): Int {
                val o1FovSize = o1.fovHorizontal * o1.fovVertical
                val o2FovSize = o2.fovHorizontal * o2.fovVertical
                if (o1FovSize > o2FovSize) {
                    return 1
                } else if (o1FovSize < o2FovSize) {
                    return -1
                }
                return 0
            }
        }

        frontCameraList.sortWith(comparator)
        backCameraList.sortWith(comparator)

        stringBuilder.appendln("front camera result:")
        frontCameraList.forEach {
            stringBuilder.appendln("id = ${it.cameraId}, size = ${it.sensorSize} fovH = ${it.fovHorizontal}")
        }
        stringBuilder.appendln("front wide camera is ${frontCameraList.last()}")
        stringBuilder.appendln()

        stringBuilder.appendln("back camera result:")
        backCameraList.forEach {
            stringBuilder.appendln("id = ${it.cameraId}, size = ${it.sensorSize} fovH = ${it.fovHorizontal}")
        }
        stringBuilder.appendln("back wide camera is ${backCameraList.last()}")

        textView.text = stringBuilder
        return stringBuilder.toString()
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            } else {
                getCameraLensInfo()
            }
        }
    }


}

data class CameraInformation(
    val cameraId: String,
    val isFrontFacing: Boolean,
    val sensorSize: SizeF,
    val fovHorizontal: Float,
    val fovVertical: Float
)
