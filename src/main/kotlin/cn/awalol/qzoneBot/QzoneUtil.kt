package cn.awalol.qzoneBot

import cn.awalol.qzoneBot.bean.qzoneSuosuo.PicInfo
import cn.awalol.qzoneBot.bean.qzoneSuosuo.PicinfoX
import cn.awalol.qzoneBot.bean.qzoneSuosuo.UploadPic
import io.ktor.client.request.*
import io.ktor.http.*
import org.openqa.selenium.Dimension
import org.openqa.selenium.chrome.ChromeDriver
import java.net.URLEncoder
import java.util.*

object QzoneUtil {
    private fun getGtk(sKey: String): Long {
        var hash: Long = 5381
        for (element in sKey) {
            hash += (hash shl 5) + element.toLong()
        }
        return hash and 0x7fffffff
    }

    fun login(){
        val driver = ChromeDriver()
        driver.manage().window().size = Dimension(305,420);
        driver.get("https://xui.ptlogin2.qq.com/cgi-bin/xlogin?proxy_url=https%3A//qzs.qq.com/qzone/v6/portal/proxy.html&daid=5&&hide_title_bar=1&low_login=0&qlogin_auto_login=1&no_verifyimg=1&link_target=blank&appid=549000912&style=22&target=self&s_url=https%3A%2F%2Fqzs.qzone.qq.com%2Fqzone%2Fv5%2Floginsucc.html%3Fpara%3Dizone&pt_qr_app=%E6%89%8B%E6%9C%BAQQ%E7%A9%BA%E9%97%B4&pt_no_auth=1")
        try {
            while (true){
                if(driver.currentUrl.contains("https://user.qzone.qq.com/")){
                    driver.manage().cookies.forEach { cookie ->
                        if(cookie.name.contentEquals("p_uin").or(cookie.name.contentEquals("p_skey"))){
                            qzoneCookie.put(if (cookie.name.contentEquals("p_uin")) "p_uin" else "p_skey",cookie.value)
                        }
                    }
                    return
                }
            }
        }finally {
            driver.quit()
        }
    }

    suspend fun publishShuoshuo(content : String, image : String) : String{
        //get image ByteArray
        var imageBase64: String
        val imageResponse : ByteArray = client.get(image)
        imageBase64 = Base64.getUrlEncoder().encodeToString(imageResponse)
        //upload Image to Qzone
        val uploadPic1Response : String = client.post{
            url("https://mobile.qzone.qq.com/up/cgi-bin/upload/cgi_upload_pic_v2?g_tk=" + getGtk(qzoneCookie.getValue("p_skey")))
            headers{
                append("cookie","p_uin=" + qzoneCookie.getValue("p_uin") + ";p_skey=" + qzoneCookie.getValue("p_skey") + ";")
                append(HttpHeaders.ContentType,"application/x-www-form-urlencoded")
            }
            body = "picture=$imageBase64&output_type=json&preupload=1&base64=1"
        }
        println(getStringMiddleContent(uploadPic1Response,"_Callback(",");"))
        val uploadPic : UploadPic = objectMapper.readValue(getStringMiddleContent(uploadPic1Response,"_Callback(",");"),
            UploadPic::class.java) //JSON反序列化

        //upload Image to ablum
        val uploadPic2Response : String = client.post{
            url("https://mobile.qzone.qq.com/up/cgi-bin/upload/cgi_upload_pic_v2?g_tk=" + getGtk(qzoneCookie.getValue("p_skey")))
            headers{
                append("cookie","p_uin=" + qzoneCookie.getValue("p_uin") + ";p_skey=" + qzoneCookie.getValue("p_skey") + ";")
                append(HttpHeaders.ContentType,"application/x-www-form-urlencoded")
            }
            body = "output_type=json&preupload=2&md5=${uploadPic.filemd5}&filelen=${uploadPic.filelen}&refer=shuoshuo&albumtype=7"
        }
        val imageContent = getStringMiddleContent(uploadPic2Response,"_Callback([","]);")
        println(imageContent)
        val picInfo : PicinfoX = objectMapper.readValue(imageContent, PicInfo::class.java).picinfo

        //publish Shuoshuo
        return client.post{
            url("https://mobile.qzone.qq.com/mood/publish_mood?g_tk=" + getGtk(qzoneCookie.getValue("p_skey")))
            headers {
                append(
                    "cookie",
                    "p_uin=" + qzoneCookie.getValue("p_uin") + ";p_skey=" + qzoneCookie.getValue("p_skey") + ";"
                )
            }
            body = "opr_type=publish_shuoshuo&content=$content&format=json&richval=" + (picInfo.albumid + "," + picInfo.sloc + "," + picInfo.lloc + ",," + picInfo.height + "," + picInfo.width + ",,,")
        }
    }

    suspend fun publishShuoshuo(content: String) : String{
        return client.post{
            url("https://mobile.qzone.qq.com/mood/publish_mood?g_tk=" + getGtk(qzoneCookie.getValue("p_skey")))
            headers {
                append(
                    "cookie",
                    "p_uin=" + qzoneCookie.getValue("p_uin") + ";p_skey=" + qzoneCookie.getValue("p_skey") + ";"
                )
            }
            body = "opr_type=publish_shuoshuo&content=${URLEncoder.encode(content)}&format=json"
        }
    }

    fun getStringMiddleContent (string: String,startString: String,endString: String) : String{
        val startIndex = string.indexOf(startString)
        val endIndex = string.indexOf(endString,startIndex)
        return string.substring(startIndex + startString.length,endIndex)
    }
}