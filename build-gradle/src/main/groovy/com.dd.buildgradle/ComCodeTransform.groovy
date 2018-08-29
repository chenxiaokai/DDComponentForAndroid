package com.dd.buildgradle

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.*
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

public class ComCodeTransform extends Transform {

    private Project project
    ClassPool classPool
    String applicationName;

    ComCodeTransform(Project project) {
        this.project = project
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        //给 applicationName 赋值
        getRealApplicationName(transformInvocation.getInputs());

        classPool = new ClassPool()
        project.android.bootClasspath.each {

            // bootClasspath = C:\Users\lenovo\AppData\Local\Android\Sdk\platforms\android-25\android.jar
            System.out.println("------> bootClasspath " + it.absolutePath);

            classPool.appendClassPath((String) it.absolutePath)
        }
        def box = ConvertUtils.toCtClasses(transformInvocation.getInputs(), classPool)

        //要收集的application，一般情况下只有一个
        List<CtClass> applications = new ArrayList<>();
        //要收集的applicationlikes，一般情况下有几个组件就有几个applicationlike
        List<CtClass> activators = new ArrayList<>();

        for (CtClass ctClass : box) {
            if (isApplication(ctClass)) {
                applications.add(ctClass)
                continue;
            }
            if (isActivator(ctClass)) {
                activators.add(ctClass)
            }
        }
        for (CtClass ctClass : applications) {
            //application is   com.mrzhang.component.application.AppApplication
            System.out.println("------> application is   " + ctClass.getName());
        }
        for (CtClass ctClass : activators) {
            //applicationlike is   com.mrzhang.reader.applike.ReaderAppLike
            //applicationlike is   com.mrzhang.share.applike.ShareApplike
            System.out.println("------> applicationlike is   " + ctClass.getName());
        }

        transformInvocation.inputs.each { TransformInput input ->
            //对类型为jar文件的input进行遍历
            input.jarInputs.each { JarInput jarInput ->
                //jar文件一般是第三方依赖库jar文件
                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())

                //jar  jarName b8dd71e9d56e88393344ed997fe09472519bddf6
                //jar  jarName 550636d2ca0aca8b983e8158e07365ab9c5ef6c5
                //...
                //...
                System.out.println("------> jar  jarName " + jarName);
                //jar  absolutePath C:\Users\lenovo\.android\build-cache\0770241c6baee621b12e6589a3e059209e5e4488\output\jars\classes.jar
                //jar  absolutePath C:\Users\lenovo\.android\build-cache\e0afca4f8974bd02a94988c17b37ebbce5d13dc6\output\jars\classes.jar
                //...
                //...  查看 根目录下 myLog.txt 日志
                System.out.println("------> jar  absolutePath " + jarInput.file.getAbsolutePath());

                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //生成输出路径
                def dest = transformInvocation.outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)

                //jar  dest E:\github\DDComponentForAndroid-master\app\build\intermediates\transforms\ComponentCode\release\jars\1\10\b8dd71e9d56e88393344ed997fe09472519bddf623215fdea045716d9ab96cd92121598e.jar
                //jar  dest E:\github\DDComponentForAndroid-master\app\build\intermediates\transforms\ComponentCode\release\jars\1\10\550636d2ca0aca8b983e8158e07365ab9c5ef6c54ae6fe53c41f2df144c2c1fbc7f1bd47.jar
                //...
                //...
                System.out.println("------> jar  dest " + dest);

                //将输入内容复制到输出
                FileUtils.copyFile(jarInput.file, dest)

            }
            //对类型为“文件夹”的input进行遍历
            input.directoryInputs.each { DirectoryInput directoryInput ->
                boolean isRegisterCompoAuto = project.extensions.combuild.isRegisterCompoAuto
                if (isRegisterCompoAuto) {  //gradle 插件参数  是否需要 javassist 字节码插入
                    String fileName = directoryInput.file.absolutePath

                    //direcotry  fileName E:\github\DDComponentForAndroid-master\app\build\intermediates\classes\release
                    System.out.println("------> direcotry  fileName " + fileName);

                    File dir = new File(fileName)
                    dir.eachFileRecurse { File file ->
                        String filePath = file.absolutePath
                        String classNameTemp = filePath.replace(fileName, "").replace("\\", ".").replace("/", ".")

                        //direcotry  filePath E:\github\DDComponentForAndroid-master\app\build\intermediates\classes\release\android
                        //direcotry  filePath E:\github\DDComponentForAndroid-master\app\build\intermediates\classes\release\android\support
                        //direcotry  filePath E:\github\DDComponentForAndroid-master\app\build\intermediates\classes\release\android\support\compat
                        //direcotry  filePath E:\github\DDComponentForAndroid-master\app\build\intermediates\classes\release\android\support\compat\R.class
                        //...
                        //direcotry  filePath E:\github\DDComponentForAndroid-master\app\build\intermediates\classes\release\android\support\fragment
                        //...
                        //direcotry  filePath E:\github\DDComponentForAndroid-master\app\build\intermediates\classes\release\com\mrzhang\component\application
                        //direcotry  filePath E:\github\DDComponentForAndroid-master\app\build\intermediates\classes\release\com\mrzhang\component\application\AppApplication.class
                        System.out.println("------> direcotry  filePath " + filePath);
                        //direcotry  classNameTemp .android
                        //direcotry  classNameTemp .android.support
                        //direcotry  classNameTemp .android.support.compat
                        //direcotry  classNameTemp .android.support.compat.R.class
                        //...
                        //direcotry  classNameTemp .android.support.fragment
                        //....
                        //direcotry  classNameTemp .com.mrzhang.component.application
                        //direcotry  classNameTemp .com.mrzhang.component.application.AppApplication.class
                        System.out.println("------> direcotry  classNameTemp " + classNameTemp);

                        if (classNameTemp.endsWith(".class")) {
                            String className = classNameTemp.substring(1, classNameTemp.length() - 6)
                            if (className.equals(applicationName)) {
                                //给应用程序 Application  注入代码
                                injectApplicationCode(applications.get(0), activators, fileName);
                            }
                        }
                    }
                }
                def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)

                //direcotry  dest E:\github\DDComponentForAndroid-master\app\build\intermediates\transforms\ComponentCode\release\folders\1\1\f8fc2e1c66fe19d1d8da18bc94e6ce45aca7829e
                System.out.println("------> direcotry  dest " + dest);

                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }
    }


    private void getRealApplicationName(Collection<TransformInput> inputs) {
        //对应 主工程  module的 gradle 文件 插件配置文件
        applicationName = project.extensions.combuild.applicatonName
        if (applicationName == null || applicationName.isEmpty()) {
            throw new RuntimeException("you should set applicationName in combuild")
        }
    }


    private void injectApplicationCode(CtClass ctClassApplication, List<CtClass> activators, String patch) {
        System.out.println("------> injectApplicationCode begin");
        ctClassApplication.defrost();
        try {
            CtMethod attachBaseContextMethod = ctClassApplication.getDeclaredMethod("onCreate", null)
            attachBaseContextMethod.insertAfter(getAutoLoadComCode(activators))
        } catch (CannotCompileException | NotFoundException e) {
            StringBuilder methodBody = new StringBuilder();
            methodBody.append("protected void onCreate() {");
            methodBody.append("super.onCreate();");
            methodBody.
                    append(getAutoLoadComCode(activators));
            methodBody.append("}");
            ctClassApplication.addMethod(CtMethod.make(methodBody.toString(), ctClassApplication));
        } catch (Exception e) {

        }
        ctClassApplication.writeFile(patch)
        ctClassApplication.detach()

        //injectApplicationCode patch = E:\github\DDComponentForAndroid-master\app\build\intermediates\classes\release
        System.out.println("------> injectApplicationCode patch = "+patch);
        //injectApplicationCode inject body = new com.mrzhang.reader.applike.ReaderAppLike().onCreate();new com.mrzhang.share.applike.ShareApplike().onCreate();
        System.out.println("------> injectApplicationCode inject body = "+getAutoLoadComCode(activators));

        System.out.println("------> injectApplicationCode success ");
    }

    private String getAutoLoadComCode(List<CtClass> activators) {
        StringBuilder autoLoadComCode = new StringBuilder();
        for (CtClass ctClass : activators) {
            autoLoadComCode.append("new " + ctClass.getName() + "()" + ".onCreate();")
        }

        return autoLoadComCode.toString()
    }


    private boolean isApplication(CtClass ctClass) {
        try {
            if (applicationName != null && applicationName.equals(ctClass.getName())) {
                return true;
            }
        } catch (Exception e) {
            println "class not found exception class name:  " + ctClass.getName()
        }
        return false;
    }

    private boolean isActivator(CtClass ctClass) {
        try {
            for (CtClass ctClassInter : ctClass.getInterfaces()) {
                if ("com.mrzhang.component.componentlib.applicationlike.IApplicationLike".equals(ctClassInter.name)) {
                    return true;
                }
            }
        } catch (Exception e) {
            println "class not found exception class name:  " + ctClass.getName()
        }

        return false;
    }

    // 设置我们自定义的Transform对应的Task名称
    //生成一个  :app:transformClassesWithComponentCodeForRelease 一个 task 然后执行 transform() 方法
    @Override
    String getName() {
        return "ComponentCode"
    }

    // 指定输入的类型，通过这里的设定，可以指定我们要处理的文件类型
    //这样确保其他类型的文件不会传入
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    // 指定Transform的作用范围
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

}