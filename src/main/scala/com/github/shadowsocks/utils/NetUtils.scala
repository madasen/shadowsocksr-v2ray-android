package com.github.shadowsocks.utils

import java.io.IOException
import java.lang.System.currentTimeMillis
import java.net.{Inet4Address, InetAddress, Socket}
import java.util
import java.util.concurrent.TimeUnit

import com.github.shadowsocks.R
import com.github.shadowsocks.ShadowsocksApplication.app
import com.github.shadowsocks.database.SSRAction.profile
import okhttp3.{Dns, OkHttpClient, Request}

import scala.util.{Success, Try}

object NetUtils {

  private val TAG = "NetUtils"

  def isPortAvailable(port: Int): Boolean = {
    // Assume no connection is possible.
    var result = true
    try {
      new Socket("127.0.0.1", port).close()
      result = false;
    } catch {
      case e: Exception => Unit
    }
    result
  }

  def testConnection(url: String, timeout: Int = 2): Long = {
    var elapsed = 0L
    val dns = new Dns {
      override def lookup(s: String): util.List[InetAddress] = {
        val address = if (!Utils.isNumeric(s)) {
          Utils.resolve(s, enableIPv6 = false, hostname="223.5.5.5") match {
            case Some(addr) => InetAddress.getByName(addr)
            case None => throw new IOException("Name Not Resolved")
          }
        } else {
          InetAddress.getByName(s)
        }
        util.Arrays.asList(address)
      }
    }
    val builder = new OkHttpClient.Builder()
      .connectTimeout(timeout, TimeUnit.SECONDS)
      .writeTimeout(timeout, TimeUnit.SECONDS)
      .readTimeout(timeout, TimeUnit.SECONDS)
      .dns(dns)
    val client = builder.build()
    val request = new Request.Builder()
      .url(url).removeHeader("Host").addHeader("Host", "www.gstatic.com")
      .build()
    val response = client.newCall(request).execute()
    val code = response.code()
    if (code == 204 || code == 200 && response.body().contentLength == 0) {
      val start = currentTimeMillis
      val response = client.newCall(request).execute()
      elapsed = currentTimeMillis - start
      val code = response.code()
      if (code == 204 || code == 200 && response.body().contentLength == 0) {
        response.body().close()
      }
      else throw new Exception(app.getString(R.string.connection_test_error_status_code, code: Integer))
    } else throw new Exception(app.getString(R.string.connection_test_error_status_code, code: Integer))
    response.body().close()
    elapsed
  }

}
