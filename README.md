# Wide-Angle-Camera

[![HitCount](http://hits.dwyl.com/charlie-captain/Multi-Camera.svg)](http://hits.dwyl.com/charlie-captain/Multi-Camera)

获取设备相机信息, 判断相机是否广角

```
//通过物理尺寸, 对焦距离, 算出FOV(Field of view), 相机水平弧度, 垂直弧度
val size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
val focalLens1 = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)!!
val w = size!!.width
val h = size.height
val horizontalAngle = (2 * Math.atan(w / (focalLens1[0] * 2).toDouble())).toFloat()
val verticalAngle = (2 * Math.atan(h / (focalLens1[0] * 2).toDouble())).toFloat()

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
```

## 参考文献
1. 三星Camera广角能力开放 http://support-cn.samsung.com/App/DeveloperChina/Notice/Detail?NoticeId=109
2. https://www.panavision.com/sites/default/files/docs/documentLibrary/2%20Sensor%20Size%20FOV%20(2).pdf
3. https://stackoverflow.com/questions/31172794/get-angle-of-view-of-android-camera-device/31640287
4. https://github.com/pchan1401-ICIL/Camera2FOV
