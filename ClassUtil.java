public class ClassUtil {
	public static String getCallerClassName(int callStackDepth) {
		return Thread.currentThread().getStackTrace()[callStackDepth].getClassName();
	}
	
	public static boolean hasDeclaredMethod(Class<?> clazz, String methodName, Class<?>... checkedParameters) {
		Method[] methods = clazz.getDeclaredMethods();
		for (Method m : methods) {
			if (m.getName().equals(methodName)) {
				Class<?>[] params = m.getParameterTypes();
				if (params.length == checkedParameters.length) {
					boolean isEqual = true;
					for (int i = 0; i < params.length; i++) {
						if (!params[i].equals(checkedParameters[i])) {
							isEqual = false;
						}
					}
					if(isEqual == true){
						return isEqual;
					}
				}
			}
		}
		return false;
	}
	
	public static <T> List<Class<? extends T>> getSubClasses(Class<T> clazz, Package pack) {
		Set<Class<?>> allClassesInPackage = getClasses(pack);
		return getSubClasses(clazz, allClassesInPackage);
	}

	private static <T> ArrayList<Class<? extends T>> getSubClasses(Class<T> clazz, Collection<Class<?>> allClassesInPackage) {
		ArrayList<Class<? extends T>> subClasses = new ArrayList<Class<? extends T>>();
		for (Class<?> tmpClass : allClassesInPackage) {
			if (clazz.isAssignableFrom(tmpClass) && !tmpClass.isAssignableFrom(clazz)) {
				subClasses.add(tmpClass.asSubclass(clazz));
			}
		}
		return subClasses;
	}
	
	public static <T> List<Class<? extends T>> getSubClasses(Class<T> clazz) {
		return getSubClasses(clazz, loadClassesFromPath());
	}
	
	/** 
	 * 从包package中获取所有的Class 
	 * @param pack 
	 * @return 
	 */
	public static Set<Class<?>> getClasses(Package pack) {
		// 第一个class类的集合
		Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
		// 是否循环迭代
		boolean recursive = true;
		// 获取包的名字 并进行替换
		String packageName = pack.getName();
		String packageDirName = packageName.replace('.', '/');
		// 定义一个枚举的集合 并进行循环来处理这个目录下的things
		Enumeration<URL> dirs;
		try {
			dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
			// 循环迭代下去
			while (dirs.hasMoreElements()) {
				// 获取下一个元素
				URL url = dirs.nextElement();
				// 得到协议的名称
				String protocol = url.getProtocol();
				// 如果是以文件的形式保存在服务器上
				if ("file".equals(protocol)) {
					// 获取包的物理路径
					String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					// 以文件的方式扫描整个包下的文件 并添加到集合中
					findAndAddClassesInPackageByFile(packageName, filePath, true, classes);
				} else if ("jar".equals(protocol)) {
					// 如果是jar包文件
					// 定义一个JarFile
					JarFile jar;
					try {
						// 获取jar
						jar = ((JarURLConnection) url.openConnection()).getJarFile();
						// 从此jar包 得到一个枚举类
						Enumeration<JarEntry> entries = jar.entries();
						String jarPackageName = packageName;
						// 同样的进行循环迭代
						while (entries.hasMoreElements()) {
							// 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
							JarEntry entry = entries.nextElement();
							String name = entry.getName();
							// 如果是以/开头的
							if (name.charAt(0) == '/') {
								// 获取后面的字符串
								name = name.substring(1);
							}
							// 如果前半部分和定义的包名相同
							if (name.startsWith(packageDirName)) {
								int idx = name.lastIndexOf('/');
								// 如果以"/"结尾 是一个包
								if (idx != -1) {
									// 获取包名 把"/"替换成"."
									jarPackageName = name.substring(0, idx).replace('/', '.');
								}
								// 如果可以迭代下去 并且是一个包
								if ((idx != -1) || recursive) {
									// 如果是一个.class文件 而且不是目录
									if (name.endsWith(".class") && !entry.isDirectory()) {
										// 去掉后面的".class" 获取真正的类名
										String className = name.substring(jarPackageName.length() + 1, name.length() - 6);
										try {
											// 添加到classes
											classes.add(Class.forName(jarPackageName + '.' + className, false, Thread.currentThread()
													.getContextClassLoader()));
										} catch (ClassNotFoundException e) {
											Logger.error("添加用户自定义视图类错误 找不到此类的.class文件");
										}
									}
								}
							}
						}
					} catch (IOException e) {
						Logger.error("在扫描用户定义视图时从jar包获取文件出错");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return classes;
	}
	
	/** 
	 * 以文件的形式来获取包下的所有Class 
	 * @param packageName 
	 * @param packagePath 
	 * @param recursive 
	 * @param classes 
	 */
	public static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Set<Class<?>> classes) {
		// 获取此包的目录 建立一个File
		File dir = new File(packagePath);
		// 如果不存在或者 也不是目录就直接返回
		if (!dir.exists() || !dir.isDirectory()) {
			Logger.error("用户定义包名 " + packageName + " 下没有任何文件");
			return;
		}
		// 如果存在 就获取包下的所有文件 包括目录
		// 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
		File[] dirfiles = dir.listFiles(file -> (recursive && file.isDirectory()) || (file.getName().endsWith(".class")));
		// 循环所有文件
		for (File file : dirfiles) {
			// 如果是目录 则继续扫描
			if (file.isDirectory()) {
				findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, classes);
			} else {
				// 如果是java类文件 去掉后面的.class 只留下类名
				String className = file.getName().substring(0, file.getName().length() - 6);
				try {
					// 添加到集合中去
					classes.add(Class.forName(packageName + '.' + className, false, Thread.currentThread().getContextClassLoader()));
				} catch (ClassNotFoundException e) {
					Logger.error("添加用户自定义视图类错误 找不到此类的.class文件:" + className);
				}
			}
		}
	}
	
	public static ArrayList<Class<?>> loadClassesFromPath() {
		ArrayList<Class<?>> result = new ArrayList<Class<?>>();
		try {
			URL resource = ClassUtil.class.getResource("/");
			URI uri = resource.toURI();
			String property = new File(uri).getPath();
			String[] paths = property.split(";");
			for (String path : paths) {
				File file = new File(path);
				if (file.isFile() && path.endsWith(".jar")) {
					result.addAll(listClassesInZip(file));
				} else if (file.isDirectory()) {
					result.addAll(listClassesInDirectory(path + File.separatorChar, file));
				}
			}
		} catch (URISyntaxException e) {
			Logger.error("Error!", e);
		}
		return result;
	}
	
	private static ArrayList<Class<?>> listClassesInDirectory(String rootPath, File file) {
		ArrayList<Class<?>> result = new ArrayList<Class<?>>();
		File[] subFiles = file.listFiles();
		for (File subFile : subFiles) {
			if (subFile.canRead()) {
				if (subFile.isFile()) {
					String path = subFile.getPath();
					if (path.endsWith(".class")) {
						try {
							String className = getClassName(path.substring(rootPath.length()));
							result.add(Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
						} catch (Throwable e) {
							Logger.error("error!", e);
						}
					} else if (path.endsWith(".jar")) {
						result.addAll(listClassesInZip(subFile));
					}
				} else if (subFile.isDirectory()) {
					result.addAll(listClassesInDirectory(rootPath, subFile));
				}
			}
		}
		return result;
	}
	
	private static ArrayList<Class<?>> listClassesInZip(File jarFile) {
		ArrayList<Class<?>> result = new ArrayList<Class<?>>();
		ZipInputStream in = null;
		try {
			in = new ZipInputStream(new FileInputStream(jarFile));
			ZipEntry ze = null;
			while ((ze = in.getNextEntry()) != null) {
				if (ze.isDirectory()) {
					continue;
				} else {
					try {
						String name = ze.getName();
						if (!name.endsWith(".class"))
							continue;
						String className = getClassName(name);
						result.add(Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
					} catch (Throwable e) {
						Logger.error("Error!", e);
					}
				}
			}
		} catch (Throwable e) {
			Logger.error("Error!", e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Logger.error("Error!", e);
				}
			}
		}
		return result;
	}
	
	private static String getClassName(String path) {
		return path.replace('/', '.').replace('\\', '.').replaceAll(".class", "");
	}
}
