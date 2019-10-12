JaCoCo Java Code Coverage Library
=================================

[![Build Status](https://travis-ci.org/jacoco/jacoco.svg?branch=master)](https://travis-ci.org/jacoco/jacoco)
[![Build status](https://ci.appveyor.com/api/projects/status/g28egytv4tb898d7/branch/master?svg=true)](https://ci.appveyor.com/project/JaCoCo/jacoco/branch/master)
[![Maven Central](https://img.shields.io/maven-central/v/org.jacoco/jacoco.svg)](http://search.maven.org/#search|ga|1|g%3Aorg.jacoco)

JaCoCo is a free Java code coverage library distributed under the Eclipse Public
License. Check the [project homepage](http://www.jacoco.org/jacoco)
for downloads, documentation and feedback.

Please use our [mailing list](https://groups.google.com/forum/?fromgroups=#!forum/jacoco)
for questions regarding JaCoCo which are not already covered by the
[extensive documentation](http://www.jacoco.org/jacoco/trunk/doc/).

Note: 欢迎一起开发，有问题提issue
-------------------------------------------------------------------------

JaCoCo二次开发基于Git分支差分实现增量代码覆盖率

- 原理：通过使用org.eclipse.jgit比较新旧分支代码差分，取得变更行信息，生成报告时高亮线上变更行信息，未检出变更行不做处理。从而达到，增量显示代码覆盖率的目的。当然当不传代码分支时默认展示全量覆盖率。

- 使用方法：

  ##### 一，Jacoco包的配置(具体配置百度Jacoco的Tomcat配置)

  ###### 1，懒伸手党模式：使用我二次开发后上传的[Release包](https://github.com/512433465/JacocoPlus/releases)代替自己下载的Jacoco包。

  ###### 2，自己动手模式：下载我的代码，执行mvn clean package -Dmaven.javadoc.test=true -Dmaven.test.skip=true自行打包。注意事项IDE下载后，在IDE中执行maven打包命令可能不成功。  建议在命令行中执行maven打包命令。打包成功后，用官网包lib/jacocoagent.jar 替换自己新打包生成的jacocoagent.jar文件，否则可能引起Tomcat无法启动。

#####      二，代码差分比较分支说明

​     [测试代码](https://github.com/512433465/JacocoTest)以这个测试代码为例,假设daily为变更后要Release的代码，master分支为基线分支。测试的目的是想测试新开发的代码也即daily分支的代码是否全部已测试覆盖。我们测试时把daily下载后打包发布完成测试（在第一步Jacoco已经再Tomcat下配置好的情况下）。

#####      三，下载jacoco.exec（这里主要考虑到集成到Devops平台，通过代码获取）

```java
package com.keking.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

public class ExecutionDataClient {
	private static final String DESTFILE = "E:\\Git-pro\\JacocoTest\\jacoco.exec";//导出的文件路径

	private static final String ADDRESS = "192.168.0.11";//配置的Jacoco的IP

	private static final int PORT = 1024;//Jacoco监听的端口

	/**
	 * Starts the execution data request.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {
		final FileOutputStream localFile = new FileOutputStream(DESTFILE);
		final ExecutionDataWriter localWriter = new ExecutionDataWriter(
				localFile);

		//连接Jacoco服务
		final Socket socket = new Socket(InetAddress.getByName(ADDRESS), PORT);
		final RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
		final RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());
		reader.setSessionInfoVisitor(localWriter);
		reader.setExecutionDataVisitor(localWriter);

		// 发送Dump命令，获取Exec数据
		writer.visitDumpCommand(true, false);
		if (!reader.read()) {
			throw new IOException("Socket closed unexpectedly.");
		}

		socket.close();
		localFile.close();
	}

	private ExecutionDataClient() {
	}
}


```
这样就非常方便的不停服务的情况下，去导出exec文件了。

##### 四，基于Git分支差分解析jacoco.exec生成报告

```java
package com.keking.report;

import java.io.File;
import java.io.IOException;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.internal.diff.GitAdapter;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.MultiSourceFileLocator;
import org.jacoco.report.html.HTMLFormatter;


/**
 * This example creates a HTML report for eclipse like projects based on a
 * single execution data store called jacoco.exec. The report contains no
 * grouping information.
 * 
 * The class files under test must be compiled with debug information, otherwise
 * source highlighting will not work.
 */
public class ReportGenerator {

	private final String title;

	private final File executionDataFile;
	private final File classesDirectory;
	private final File sourceDirectory;
	private final File reportDirectory;

	private ExecFileLoader execFileLoader;

	/**
	 * Create a new generator based for the given project.
	 * 
	 * @param projectDirectory
	 */
	public ReportGenerator(final File projectDirectory) {
		this.title = projectDirectory.getName();
		this.executionDataFile = new File(projectDirectory, "jacoco.exec");//第一步生成的exec的文件
		this.classesDirectory = new File(projectDirectory, "bin");//目录下必须包含源码编译过的class文件,用来统计覆盖率。所以这里用server打出的jar包地址即可,运行的jar或者Class目录
		this.sourceDirectory = new File(projectDirectory, "src/main/java");//源码目录
		this.reportDirectory = new File(projectDirectory, "coveragereport");////要保存报告的地址
	}

	/**
	 * Create the report.
	 * 
	 * @throws IOException
	 */
	public void create() throws IOException {

		// Read the jacoco.exec file. Multiple data files could be merged
		// at this point
		loadExecutionData();

		// Run the structure analyzer on a single class folder to build up
		// the coverage model. The process would be similar if your classes
		// were in a jar file. Typically you would create a bundle for each
		// class folder and each jar you want in your report. If you have
		// more than one bundle you will need to add a grouping node to your
		// report
		final IBundleCoverage bundleCoverage = analyzeStructure();

		createReport(bundleCoverage);

	}

	private void createReport(final IBundleCoverage bundleCoverage)
			throws IOException {

		// Create a concrete report visitor based on some supplied
		// configuration. In this case we use the defaults
		final HTMLFormatter htmlFormatter = new HTMLFormatter();
		final IReportVisitor visitor = htmlFormatter.createVisitor(new FileMultiReportOutput(reportDirectory));

		// Initialize the report with all of the execution and session
		// information. At this point the report doesn't know about the
		// structure of the report being created
		visitor.visitInfo(execFileLoader.getSessionInfoStore().getInfos(),execFileLoader.getExecutionDataStore().getContents());

		// Populate the report structure with the bundle coverage information.
		// Call visitGroup if you need groups in your report.
		visitor.visitBundle(bundleCoverage, new DirectorySourceFileLocator(sourceDirectory, "utf-8", 4));
		
		
//		//多源码路径
//        MultiSourceFileLocator sourceLocator = new MultiSourceFileLocator(4);
//        sourceLocator.add( new DirectorySourceFileLocator(sourceDirectory1, "utf-8", 4));
//        sourceLocator.add( new DirectorySourceFileLocator(sourceDirectory2, "utf-8", 4));
//        sourceLocator.add( new DirectorySourceFileLocator(sourceDirectoryN, "utf-8", 4));
//        visitor.visitBundle(bundleCoverage,sourceLocator);

		// Signal end of structure information to allow report to write all
		// information out
		visitor.visitEnd();

	}

	private void loadExecutionData() throws IOException {
		execFileLoader = new ExecFileLoader();
		execFileLoader.load(executionDataFile);
	}

	private IBundleCoverage analyzeStructure() throws IOException {
		//git登录授权
		GitAdapter.setCredentialsProvider("QQ512433465", "mima512433465");
		//全量覆盖
//		final CoverageBuilder coverageBuilder = new CoverageBuilder();
        
        
        //基于分支比较覆盖，参数1：本地仓库，参数2：开发分支（预发分支），参数3：基线分支(不传时默认为master)
        //本地Git路径，新分支 第三个参数不传时默认比较maser，传参数为待比较的基线分支
		final CoverageBuilder coverageBuilder = new CoverageBuilder("E:\\Git-pro\\JacocoTest","daily");
        
        //基于Tag比较的覆盖 参数1：本地仓库，参数2：代码分支，参数3：新Tag(预发版本)，参数4：基线Tag（变更前的版本）
        //final CoverageBuilder coverageBuilder = new CoverageBuilder("E:\\Git-pro\\JacocoTest","daily","v004","v003");
		
		
		final Analyzer analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);

		analyzer.analyzeAll(classesDirectory);

		return coverageBuilder.getBundle(title);
	}

	/**
	 * Starts the report generation process
	 * 
	 * @param args
	 *            Arguments to the application. This will be the location of the
	 *            eclipse projects that will be used to generate reports for
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {

		final ReportGenerator generator = new ReportGenerator(new File("E:\\Git-pro\\JacocoTest"));
		generator.create();
	}

}


```
执行完后就可以生成报告了。通过第一步，第二步结合，就可以随时导出Ecec文件，生成报告，查看测试覆盖情况了。那种需要启停服务的

##### 五 生成的差分报告展示

通过率概览：

![图片](https://github.com/512433465/JacocoTest/blob/master/Jacocorep1.png)

![图片](https://github.com/512433465/JacocoTest/blob/master/Jacocorep11.png)

覆盖的代码显示为绿色

![图片](https://github.com/512433465/JacocoTest/blob/master/Jacocorep2.png)

未覆盖的代码显示为红色

![图片](https://github.com/512433465/JacocoTest/blob/master/Jacocorep3.png)