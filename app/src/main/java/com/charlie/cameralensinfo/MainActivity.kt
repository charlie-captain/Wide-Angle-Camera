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

        val frontCameraList = arrayListOf<CameraSensorInfo>()
        val backCameraList = arrayListOf<CameraSensorInfo>()

        cameraIdList.forEach { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            //前置
            val front = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            stringBuilder.appendln("camera id = $id: front = $front")
            //获取相机的物理尺寸
            val size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            stringBuilder.appendln("sensor info : width =  ${size?.width}, height = ${size?.height}")
            stringBuilder.appendln()
            if (front) {
                frontCameraList.add(CameraSensorInfo(cameraId = id, cameraSensorSize = size!!))
            } else {
                backCameraList.add(CameraSensorInfo(cameraId = id, cameraSensorSize = size!!))
            }
        }

//        for (i in 0..10) {
//            //模拟10个摄像头的判断
//            var iW = (Random.nextInt(1, 10)).toFloat()
//            var iH = (Random.nextInt(1, 10)).toFloat()
//            if (i % 2 == 0) {
//                frontCameraList.add(CameraSensorInfo(i.toString(), cameraSensorSize = SizeF(iW, iH)))
//            } else {
//                backCameraList.add(CameraSensorInfo(i.toString(), cameraSensorSize = SizeF(iW, iH)))
//            }
//        }

        //简单判断哪个是广角镜头
        //比较传感器物理尺寸, 尺寸越大, 越有可能是广角
        //升序比较, 尺寸从小到大
        val comparator = object : Comparator<CameraSensorInfo> {
            override fun compare(o1: CameraSensorInfo, o2: CameraSensorInfo): Int {
                val o1Size = o1.cameraSensorSize
                val o2Size = o2.cameraSensorSize
                if (o1Size.width > o2Size.width) {
                    return if (o1Size.height > o2Size.height) {
                        1
                    } else {
                        0
                    }
                }
                if (o1Size.width < o2Size.width) {
                    return if (o1Size.height < o2Size.height) {
                        -1
                    } else {
                        0
                    }
                }
                return 0
            }
        }

        frontCameraList.sortWith(comparator)
        backCameraList.sortWith(comparator)

        stringBuilder.appendln("front camera result:")
        frontCameraList.forEach {
            stringBuilder.appendln("id = ${it.cameraId}, size = ${it.cameraSensorSize}")
        }
        stringBuilder.appendln()

        stringBuilder.appendln("back camera result:")
        backCameraList.forEach {
            stringBuilder.appendln("id = ${it.cameraId}, size = ${it.cameraSensorSize}")
        }

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


    data class CameraSensorInfo(val cameraId: String, val cameraSensorSize: SizeF)

}