package com.wpf.utils.jiagu

import java.io.File

internal fun compileResourceFixture(work: File, sdk: File, manifest: File, output: File) {
    val res = File(work, "res").apply { mkdirs() }
    fun source(path: String, contents: String) = File(res, path).apply {
        parentFile.mkdirs(); writeText(contents, Charsets.UTF_8)
    }
    source("values/strings.xml", """<resources><string name="resource_probe">资源保护 ✓</string>
        <item type="drawable" name="bootstrap_alias">@drawable/bootstrap_bg</item>
        <style name="BootstrapTheme" parent="@android:style/Theme.Material.Light.NoActionBar"><item name="android:windowBackground">@drawable/bootstrap_alias</item></style>
        </resources>""")
    source("layout/resource_probe.xml", """<TextView xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/resource_probe" android:background="@drawable/business_image"/>""")
    source("drawable/resource_probe.xml", """<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle"><solid android:color="#123456"/></shape>""")
    source("raw/resource_probe.txt", "resource raw probe")
    source("drawable/bootstrap_bg.xml", """<bitmap xmlns:android="http://schemas.android.com/apk/res/android" android:src="@drawable/bootstrap_image"/>""")
    source("drawable-night/bootstrap_bg.xml", """<bitmap xmlns:android="http://schemas.android.com/apk/res/android" android:src="@drawable/bootstrap_image"/>""")
    val bitmap = java.awt.image.BufferedImage(4, 3, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    for (x in 0 until 4) for (y in 0 until 3) bitmap.setRGB(x, y, 0xff123456.toInt())
    listOf("business_image", "bootstrap_image", "bootstrap_icon").forEach { name ->
        javax.imageio.ImageIO.write(bitmap, "png", File(res, "drawable/$name.png"))
    }
    val assets = File(work, "fixture-assets").apply { mkdirs() }
    File(assets, "resource_probe.txt").writeText("resource asset probe")
    File(assets, "resource_large.bin").writeBytes(ByteArray(300000) { (it % 251).toByte() })
    val aapt = File(sdk, "build-tools/35.0.0/aapt2.exe")
    fun run(vararg args: String) {
        val p = ProcessBuilder(listOf(aapt.path) + args).redirectErrorStream(true).start()
        val message = p.inputStream.bufferedReader().readText()
        check(p.waitFor() == 0) { message }
    }
    val compiled = File(work, "compiled-res.zip")
    run("compile", "--dir", res.path, "-o", compiled.path)
    run("link", "--manifest", manifest.path, "-I", File(sdk, "platforms/android-33/android.jar").path,
        "-A", assets.path, "-0", "txt", "-o", output.path, compiled.path)
}
