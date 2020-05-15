## Spring容器之Resource 和 ResourceLoader
&ensp;&ensp;这篇文章是填之前文章的坑来了，首先在XmlBeanFactory中，有这么一行代码 `new XmlBeanFactory(new ClassPathResource("spring-bean.xml"));`
其中的 `new ClassPathResource("spring-bean.xml")`没有解释，这就是spring中的**Resource**。

&ensp;&ensp;其次，在 DefaultListableBeanFactory中，`new XmlBeanDefinitionReader(factory)`，中完成了对ResourceLoader的初始化，所谓的ResourceLoader
就是对 Spring 资源加载的统一抽象。

&ensp;&ensp;在这篇文章中，对Spring中的资源，与资源的加载做一个统一学习。

### new ClassPathResource("xxx") 做了什么

#### ClassPathResource 的类关系图


其中AbstractResource 为 Resource的默认实现。
- InputStreamSource 封装任何返货 InputStream 的类，比如File，Classpath下的资源和Byte，Array等。
- Resource 接口抽象了所有Spring内部使用到的底层资源：File，URL，Classpath等。
- ClassPathResource 用来加载classpath 类型资源的实现。使用给定的 ClassLoader 或者给定的 Class 来加载资源。
- ByteArrayResource 对字节数组提供的数据的封装。
- FileSystemResource 文件相关。
- UrlResource url资源的加载。

AbstractResource 源码如下：
```java
public abstract class AbstractResource implements Resource {

	/**
	 * 判断文件是否存在，若判断过程产生异常（因为会调用SecurityManager来判断），就关闭对应的流
	 */
	@Override
	public boolean exists() {
		// Try file existence: can we find the file in the file system?
		try {
			return getFile().exists();
		}
		catch (IOException ex) {
			// Fall back to stream existence: can we open the stream?
			try {
				getInputStream().close();
				return true;
			}
			catch (Throwable isEx) {
				return false;
			}
		}
	}

	/**
	 * 这个重写的方法始终返回true，表示可读
	 */
	@Override
	public boolean isReadable() {
		return exists();
	}

	/**
	 * 这个重写方法始终返回false，表示没有打开
	 */
	@Override
	public boolean isOpen() {
		return false;
	}

	/**
	 * 这个从写的方法始终返回false，表示不是一个文件
	 */
	@Override
	public boolean isFile() {
		return false;
	}

	/**
	 * 抛出 FileNotFoundException 异常
	 */
	@Override
	public URL getURL() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
	}

	/**
	 * 基于 getURL() 返回的 URL 构建 URI
	 */
	@Override
	public URI getURI() throws IOException {
		URL url = getURL();
		try {
			return ResourceUtils.toURI(url);
		}
		catch (URISyntaxException ex) {
			throw new NestedIOException("Invalid URI [" + url + "]", ex);
		}
	}

	/**
	 * 抛出 FileNotFoundException 异常，交给子类实现
	 */
	@Override
	public File getFile() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
	}

	/**
	 * 根据 getInputStream() 的返回结果构建 ReadableByteChannel
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * 获取资源的长度
	 * 这个资源内容长度实际就是资源的字节长度，通过全部读取一遍来判断
	 */
	@Override
	public long contentLength() throws IOException {
		InputStream is = getInputStream();
		try {
			long size = 0;
			byte[] buf = new byte[256];
			int read;
			while ((read = is.read(buf)) != -1) {
				size += read;
			}
			return size;
		}
		finally {
			try {
				is.close();
			}
			catch (IOException ex) {
			}
		}
	}

	/**
	 * 返回资源最后的修改时间
	 */
	@Override
	public long lastModified() throws IOException {
		File fileToCheck = getFileForLastModifiedCheck();
		long lastModified = fileToCheck.lastModified();
		if (lastModified == 0L && !fileToCheck.exists()) {
			throw new FileNotFoundException(getDescription() +
					" cannot be resolved in the file system for checking its last-modified timestamp");
		}
		return lastModified;
	}

	protected File getFileForLastModifiedCheck() throws IOException {
		return getFile();
	}
	
	@Override
	public Resource createRelative(String relativePath) throws IOException {
		throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
	}

	/**
	 * 获取资源名称，默认返回 null
	 */
	@Override
	@Nullable
	public String getFilename() {
		return null;
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof Resource &&
				((Resource) other).getDescription().equals(getDescription())));
	}
	
	@Override
	public int hashCode() {
		return getDescription().hashCode();
	}

	@Override
	public String toString() {
		return getDescription();
	}

}
```

### ResourceLoader
&ensp;&ensp; 通过`new PathMatchingResourcePatternResolver()`来完成`ResourceLoader`实例化。我们还是从`ResourceLoader`的类图入手，
可以看到 `DefaultResourceLoader` 为 `ResourceLoader` 默认实现。然后 `ResourceLoader` 为所有 `Spring` IoC 容器的父接口。

ResourceLoader_class.png

下面再来看看 `ResourceLoader` 与 `DefaultResourceLoader` 中的代码：

```java
public interface ResourceLoader {

	/** 用于从类路径加载的伪URL前缀： "classpath:" */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;

	/**
	 * 根据所提供资源的路径 location 返回 Resource 实例
	 */
	Resource getResource(String location);

	/**
	 * 返回 ClassLoader 实例，对于想要获取 ResourceLoader
	 * 使用的 ClassLoader 用户来说，可以直接调用该方法来获取
	 */
	@Nullable
	ClassLoader getClassLoader();

}
```
```java
public class DefaultResourceLoader implements ResourceLoader {

	@Nullable
	private ClassLoader classLoader;

	private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<>(4);

	private final Map<Class<?>, Map<Resource, ?>> resourceCaches = new ConcurrentHashMap<>(4);


	/**
	 * 创建默认的 ClassLoader
	 * Create a new DefaultResourceLoader.
	 * <p>ClassLoader access will happen using the thread context class loader
	 * at the time of this ResourceLoader's initialization.
	 * @see java.lang.Thread#getContextClassLoader()
	 */
	public DefaultResourceLoader() {
		this.classLoader = ClassUtils.getDefaultClassLoader();
	}

	/**
	 * Create a new DefaultResourceLoader.
	 * @param classLoader the ClassLoader to load class path resources with, or {@code null}
	 * for using the thread context class loader at the time of actual resource access
	 */
	public DefaultResourceLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * Specify the ClassLoader to load class path resources with, or {@code null}
	 * for using the thread context class loader at the time of actual resource access.
	 * <p>The default is that ClassLoader access will happen using the thread context
	 * class loader at the time of this ResourceLoader's initialization.
	 */
	public void setClassLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Return the ClassLoader to load class path resources with.
	 * <p>Will get passed to ClassPathResource's constructor for all
	 * ClassPathResource objects created by this resource loader.
	 * @see ClassPathResource
	 */
	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 添加protocolResolvers
	 * Register the given resolver with this resource loader, allowing for
	 * additional protocols to be handled.
	 * <p>Any such resolver will be invoked ahead of this loader's standard
	 * resolution rules. It may therefore also override any default rules.
	 * @since 4.3
	 * @see #getProtocolResolvers()
	 */
	public void addProtocolResolver(ProtocolResolver resolver) {
		Assert.notNull(resolver, "ProtocolResolver must not be null");
		this.protocolResolvers.add(resolver);
	}

	/**
	 * Return the collection of currently registered protocol resolvers,
	 * allowing for introspection as well as modification.
	 * @since 4.3
	 */
	public Collection<ProtocolResolver> getProtocolResolvers() {
		return this.protocolResolvers;
	}

	/**
	 * Obtain a cache for the given value type, keyed by {@link Resource}.
	 * @param valueType the value type, e.g. an ASM {@code MetadataReader}
	 * @return the cache {@link Map}, shared at the {@code ResourceLoader} level
	 * @since 5.0
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<Resource, T> getResourceCache(Class<T> valueType) {
		return (Map<Resource, T>) this.resourceCaches.computeIfAbsent(valueType, key -> new ConcurrentHashMap<>());
	}

	/**
	 * Clear all resource caches in this resource loader.
	 * @since 5.0
	 * @see #getResourceCache
	 */
	public void clearResourceCaches() {
		this.resourceCaches.clear();
	}


	/**
	 * 取得Resource 的具体过程
	 * @param location the resource location
	 * @return
	 */
	@Override
	public Resource getResource(String location) {
		Assert.notNull(location, "Location must not be null");

		for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
			// 看有没有自定义的ProtocolResolver，
			// 如果有则先根据自定义的ProtocolResolver解析location得到Resource
			Resource resource = protocolResolver.resolve(location, this);
			if (resource != null) {
				return resource;
			}
		}

		if (location.startsWith("/")) {
			return getResourceByPath(location);
		}
		/**处理带有classpath 表示的Resource*/
		else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		}
		else {
			try {
				// Try to parse the location as a URL...
				/** 处理URL 标识的Resource 定位*/
				URL url = new URL(location);
				return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
			}
			catch (MalformedURLException ex) {
				// No URL -> resolve as resource path.
				/**
				 * 如果既不是classpath，也不是URL标识的Resource定位，则把getResource的
				 * 重任交给getResourcePath()，这个是一个 protected 的方法，默认的实现是
				 * 得到一个 ClassPathContextResource，这个方法常常会用子类来实现
				 * 例如: FileSystemXmlApplicationContext#getResourceByPath()
				 */
				return getResourceByPath(location);
			}
		}
	}

	/**
	 * Return a Resource handle for the resource at the given path.
	 * <p>The default implementation supports class path locations. This should
	 * be appropriate for standalone implementations but can be overridden,
	 * e.g. for implementations targeted at a Servlet container.
	 * @param path the path to the resource
	 * @return the corresponding Resource handle
	 * @see ClassPathResource
	 * @see org.springframework.context.support.FileSystemXmlApplicationContext#getResourceByPath
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#getResourceByPath
	 */
	protected Resource getResourceByPath(String path) {
		return new ClassPathContextResource(path, getClassLoader());
	}


	/**
	 * ClassPathResource that explicitly expresses a context-relative path
	 * through implementing the ContextResource interface.
	 * 通过实现ContextResource接口 明确表示下文相关路径的ClassPathResource，此方法重点在于实现ContextResource
	 */
	protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

		// 通过调用父类的构造方法创建
		public ClassPathContextResource(String path, @Nullable ClassLoader classLoader) {
			super(path, classLoader);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

		@Override
		public Resource createRelative(String relativePath) {
			String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
			return new ClassPathContextResource(pathToUse, getClassLoader());
		}
	}
}
```


#### PathMatchingResourcePatternResolver 的类关系图


- ResourceLoader 用来加载Spring定义的资源
- DefaultResourceLoader ResourceLoader默认的实现
- ResourcePatternResolver ResourcePatternResolver 是 ResourceLoader 的扩展，它支持根据指定的资源路径匹配模式每次返回多个 Resource 实例。
- PathMatchingResourcePatternResolver 为 ResourcePatternResolver 最常用的子类，它除了支持 ResourceLoader 和 ResourcePatternResolver 新增的 classpath*: 前缀外，还支持 Ant 风格的路径匹配模式（类似于 **/*.xml）。 




