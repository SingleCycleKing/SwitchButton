#SwitchButton
![Screenshot](https://github.com/SingleCycleKing/SwitchButton/blob/master/1.png)
![Screenshot](https://github.com/SingleCycleKing/SwitchButton/blob/master/1.png)
##使用方法
  1.将library导入，修改你的工程的gradle如下:

    dependencies {
      compile project(':library')
    }

  2.在xml中使用:
  ```xml
   <com.singlecycle.library.SwitchButton xmlns:app="http://schemas.android.com/apk/res-auto"
        android:id="@+id/switch_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:cursor="@drawable/switch_circle"
        android:layout_centerInParent="true"
        android:background="@drawable/background"
        app:selectedBackground="#ffe6818c"
        app:status="OFF"
        app:trackWidth="140dp"
        app:unselectedBackground="#e6e6e6" />
  ```      
  3.在你的activity中:
  ```java
 SwitchButton switchButton = (SwitchButton) findViewById(R.id.switch_button);
        switchButton.setOnStatusChangeListener(new SwitchButton.OnStatusChangeListener() {
            @Override
            public void onChange(SwitchButton.STATUS status) {
                
            }
        });
  ```
  
##说明
  Coder是枚大二狗，不足之处请多多指教～求star～
