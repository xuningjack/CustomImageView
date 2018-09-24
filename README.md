# CustomImageView
Custom Image View（自定义圆形、圆角图片）
效果图：  
![Alt text](https://github.com/xuningjack/CustomImageView/blob/master/image/0.jpg)    

使用：配置如下
        <!--正常图-->
        <ImageView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_margin="10dp"
            android:src="@drawable/icon"/>

        <!--圆角图-->
        <com.example.customview.view.CustomImageView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_margin="10dp"
            jack:src="@drawable/icon"
            jack:type="circle"/>

        <!--圆形图-->
        <com.example.customview.view.CustomImageView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_margin="10dp"
            jack:borderRadius="10dp"
            jack:src="@drawable/icon"
            jack:type="round"/>

        <!--网络图-->
        <com.example.customview.view.CustomImageView
            android:id="@+id/customImageView"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_margin="10dp"
            jack:borderRadius="20dp"
            jack:type="url"
            jack:src="@drawable/icon"
            jack:url_src="https://www.baidu.com/img/bd_logo1.png"/>

