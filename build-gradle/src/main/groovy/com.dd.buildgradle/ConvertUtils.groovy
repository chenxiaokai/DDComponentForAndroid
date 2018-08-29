package com.dd.buildgradle

import com.android.SdkConstants
import com.android.build.api.transform.TransformInput
import javassist.ClassPool
import javassist.CtClass

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Matcher

class ConvertUtils {
    static List<CtClass> toCtClasses(Collection<TransformInput> inputs, ClassPool classPool) {
        List<String> classNames = new ArrayList<>()
        List<CtClass> allClass = new ArrayList<>();
        inputs.each {
            it.directoryInputs.each {
                def dirPath = it.file.absolutePath

                //dirPath = E:\github\DDComponentForAndroid-master\app\build\intermediates\classes\release
                System.out.println("------>dirPath = " + dirPath);

                classPool.insertClassPath(it.file.absolutePath)
                org.apache.commons.io.FileUtils.listFiles(it.file, null, true).each {
                    if (it.absolutePath.endsWith(SdkConstants.DOT_CLASS)) {
                        def className = it.absolutePath.substring(dirPath.length() + 1, it.absolutePath.length() - SdkConstants.DOT_CLASS.length()).replaceAll(Matcher.quoteReplacement(File.separator), '.')

                        // 看工程下 myLog.txt  日志
                        System.out.println("------>it.absolutePath = " + it.absolutePath);
                        System.out.println("------> directory className = " + className);

                        if (classNames.contains(className)) {
                            throw new RuntimeException("You have duplicate classes with the same name : " + className + " please remove duplicate classes ")
                        }
                        classNames.add(className)
                    }
                }
            }

            it.jarInputs.each {
                classPool.insertClassPath(it.file.absolutePath)

                //it.file.absolutePath = C:\Users\lenovo\.android\build-cache\0770241c6baee621b12e6589a3e059209e5e4488\output\jars\classes.jar
                //it.file.absolutePath = C:\Users\lenovo\.android\build-cache\e0afca4f8974bd02a94988c17b37ebbce5d13dc6\output\jars\classes.jar
                //it.file.absolutePath = C:\Users\lenovo\.android\build-cache\22fd5e22242a35e9328c11563de04c753de81ca0\output\jars\classes.jar
                //it.file.absolutePath = C:\Users\lenovo\.android\build-cache\94c24ba539216564b456637f8964706bb4d36361\output\jars\classes.jar
                //it.file.absolutePath = C:\Users\lenovo\AppData\Local\Android\sdk\extras\android\m2repository\com\android\support\support-annotations\26.0.0-alpha1\support-annotations-26.0.0-alpha1.jar
                //it.file.absolutePath = C:\Users\lenovo\.gradle\caches\modules-2\files-2.1\com.squareup.okio\okio\1.9.0\f824591a0016efbaeddb8300bee54832a1398cfa\okio-1.9.0.jar
                //it.file.absolutePath = C:\Users\lenovo\.gradle\caches\modules-2\files-2.1\com.squareup.picasso\picasso\2.5.2\7446d06ec8d4f7ffcc53f1da37c95f200dcb9387\picasso-2.5.2.jar
                //it.file.absolutePath = C:\Users\lenovo\.android\build-cache\127bccdfb4b3756af8776f06d43bce72632e2301\output\jars\classes.jar
                //it.file.absolutePath = C:\Users\lenovo\.android\build-cache\f74722edf53f03b4191debba90afc7ac1dc07662\output\jars\classes.jar
                //it.file.absolutePath = C:\Users\lenovo\.android\build-cache\17f270c544ecd32a80fa5efab07beb8f84b7d59b\output\jars\classes.jar
                //it.file.absolutePath = C:\Users\lenovo\.android\build-cache\5c9935671cbcda969106e11bed450ebc650b2775\output\jars\classes.jar
                //it.file.absolutePath = C:\Users\lenovo\.android\build-cache\ae58815673e75ae48b582994a65f142ecb07e176\output\jars\classes.jar
                //it.file.absolutePath = C:\Users\lenovo\.gradle\caches\modules-2\files-2.1\com.squareup.okhttp3\okhttp\3.4.1\c7c4f9e35c2fd5900da24f9872e3971801f08ce0\okhttp-3.4.1.jar
                //it.file.absolutePath = E:\github\DDComponentForAndroid-master\readercomponent\build\intermediates\bundles\default\classes.jar
                //it.file.absolutePath = E:\github\DDComponentForAndroid-master\sharecomponent\build\intermediates\bundles\default\classes.jar
                //it.file.absolutePath = E:\github\DDComponentForAndroid-master\componentservice\build\intermediates\bundles\default\classes.jar
                //it.file.absolutePath = E:\github\DDComponentForAndroid-master\componentlib\build\intermediates\bundles\default\classes.jar
                //it.file.absolutePath = E:\github\DDComponentForAndroid-master\basicres\build\intermediates\bundles\default\classes.jar
                //it.file.absolutePath = E:\github\DDComponentForAndroid-master\basiclib\build\intermediates\bundles\default\classes.jar
                System.out.println("------>it.file.absolutePath = " + it.file.absolutePath);

                def jarFile = new JarFile(it.file)

                //it.file = C:\Users\lenovo\.android\build-cache\0770241c6baee621b12e6589a3e059209e5e4488\output\jars\classes.jar
                //it.file = C:\Users\lenovo\.android\build-cache\e0afca4f8974bd02a94988c17b37ebbce5d13dc6\output\jars\classes.jar
                //...
                //...  跟上面对应
                System.out.println("------>it.file = " + it.file);

                Enumeration<JarEntry> classes = jarFile.entries();
                while (classes.hasMoreElements()) {
                    JarEntry libClass = classes.nextElement();
                    String className = libClass.getName();
                    if (className.endsWith(SdkConstants.DOT_CLASS)) {

                        System.out.println("------> jar className = " + className);

                        className = className.substring(0, className.length() - SdkConstants.DOT_CLASS.length()).replaceAll('/', '.')  //去次.class 后缀 和  所有 / 替换成 .

                        System.out.println("------> jar className2 = " + className);

                        if (classNames.contains(className)) {
                            throw new RuntimeException("You have duplicate classes with the same name : " + className + " please remove duplicate classes ")
                        }
                        classNames.add(className)
                    }
                }
            }
        }
        classNames.each {
            try {
                allClass.add(classPool.get(it));
            } catch (javassist.NotFoundException e) {
                println "class not found exception class name:  $it "
            }
        }
        return allClass;
    }


}