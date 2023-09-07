package com.youlai.system.common.nginx;

import com.github.odiszapc.nginxparser.NgxConfig;
import com.github.odiszapc.nginxparser.NgxDumper;
import com.youlai.system.common.exception.NginxServiceManagerException;
import com.youlai.system.config.Configer;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Nginx 配置工具类
 * @Project : nginx-gui
 * @Program Name : com.aiyi.server.manager.nginx.common.NginxUtils.java
 * @Description : 
 * @Author : 郭胜凯
 * @Creation Date : 2018年2月22日 下午6:20:18
 * @ModificationHistory Who When What ---------- ------------- -----------------------------------
 *                      郭胜凯 2018年2月22日 create
 */
public class NginxUtils {

	/**
	 * 读配置
	 * @Description : 
	 * @return : NgxConfig
	 * @Creation Date : 2018年2月22日 下午6:20:29
	 * @Author : 郭胜凯
	 */
	public static NgxConfig read(String path) {
		try (InputStream stream = new FileInputStream(Configer.getNginxConfPath(path))){
			return NgxConfig.read(stream);
		} catch (IOException e) {
			throw new NginxServiceManagerException("读取Nginx配置文件失败");
		}
	}
	
	/**
	 * 写配置
	 * @Description : 
	 * @return : void
	 * @Creation Date : 2018年2月23日 下午7:32:26
	 * @Author : 郭胜凯
	 */
	public static void save(NgxConfig conf) {
		try(FileOutputStream out = new FileOutputStream(Configer.getNginxConfPath())) {
			String s = toString(conf);
			out.write(s.getBytes(StandardCharsets.UTF_8));
			out.flush();
		} catch (Exception e) {
			throw new NginxServiceManagerException("Nginx配置文件写入失败:" + e.getMessage(), e);
		}
	}
	
	
	/**
	 * 配置到文本
	 * @Description : 
	 * @return : String
	 * @Creation Date : 2018年2月23日 下午7:32:38
	 * @Author : 郭胜凯
	 */
	public static String toString(NgxConfig conf) {
		if (null == conf) {
			throw new NginxServiceManagerException("不能写入空配置");
		}
		NgxDumper dumper = new NgxDumper(conf);
		return dumper.dump();
	}
	
	/**
	 * 写配置
	 * @Description : 
	 * @return : void
	 * @Creation Date : 2018年2月23日 下午7:33:44
	 * @Author : 郭胜凯
	 */
	public static void save(String conf,String path) {
		try(FileOutputStream out = new FileOutputStream(Configer.getNginxConfPath(path))) {
			out.write(conf.getBytes(StandardCharsets.UTF_8));
			out.flush();
		} catch (Exception e) {
			throw new NginxServiceManagerException("Nginx配置文件写入失败:" + e.getMessage(), e);
		}
	}
	
	/**
	 * 校验Nginx配置文件
	 * @Description : 
	 * @return : void
	 * @Creation Date : 2018年2月26日 上午9:59:18
	 * @Author : 郭胜凯
	 */
	public static void check(String confText,String path) {
		String confPath = Configer.getNginxConfPath(path) + ".temp.check";
		try(FileOutputStream out = new FileOutputStream(confPath)) {
			out.write(confText.getBytes(StandardCharsets.UTF_8));
			out.flush();
		} catch (Exception e) {
			throw new NginxServiceManagerException("Nginx临时配置写入配置失败:" + e.getMessage(), e);
		}

		//校验
		try {
			String check = CMDUtil.excuse(CommonFields.NGINX + " -t -c " + confPath, Configer.getNginxPath());
			if (check.indexOf(CommonFields.NGINX  + ": configuration file " + confPath + " test is successful") == -1) {
				throw new NginxServiceManagerException("Nginx配置文件校验失败:" + check);
			}
		}finally {
			new File(confPath).delete();
		}
	}
}
