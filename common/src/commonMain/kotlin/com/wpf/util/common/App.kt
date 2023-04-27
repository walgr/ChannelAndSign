package com.wpf.util.common

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpf.util.common.ui.base.Menu
import com.wpf.util.common.ui.channelset.channelPage
import com.wpf.util.common.ui.configset.configPage
import com.wpf.util.common.ui.mainBgColor
import com.wpf.util.common.ui.signset.signPage

@Preview
@Composable
fun MainView(window: ComposeWindow) {

    val menuList = remember {
        mutableStateListOf(
            Menu("渠道打包", true),
            Menu("签名配置"),
            Menu("软件配置")
        )
    }

    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(color = mainBgColor)
        ) {
            Row {
                Box(
                    modifier = Modifier.requiredWidth(150.dp)
                        .fillMaxHeight()
                        .background(color = Color(28, 30, 46))
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("WPF", fontSize = 36.sp, color = Color.White)
                        }
                        LazyColumn {
                            items(count = menuList.size) { pos ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        modifier = if (menuList[pos].isSelectState.value) Modifier.fillMaxWidth()
                                            .height(56.dp)
                                            .background(Color(1f, 1f, 1f, 0.2f))
                                        else Modifier.fillMaxWidth().height(56.dp),
                                    )
                                    Text(menuList[pos].menuName, modifier = Modifier
                                        .clickable {
                                            menuList.forEach { item ->
                                                item.isSelect = false
                                                item.isSelectState.value = false
                                            }
                                            menuList[pos].isSelect = true
                                            menuList[pos].isSelectState.value = true
                                        }, fontSize = 16.sp, color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(all = 12.dp)
                        .clip(shape = RoundedCornerShape(8.dp))
                        .background(color = Color(226, 228, 246))
                ) {
                    Column {
                        //渠道配置
                        Box(
                            modifier = if (menuList[0].isSelectState.value) Modifier.fillMaxSize() else Modifier.height(
                                0.dp
                            )
                        ) {
                            channelPage(window)
                        }
                        //签名配置
                        Box(
                            modifier = if (menuList[1].isSelectState.value) Modifier.fillMaxSize() else Modifier.height(
                                0.dp
                            )
                        ) {
                            signPage(window)
                        }
                        //软件配置
                        Box(
                            modifier = if (menuList[2].isSelectState.value) Modifier.fillMaxSize() else Modifier.height(
                                0.dp
                            )
                        ) {
                            configPage()
                        }
                    }
                }

            }
        }
    }

}
