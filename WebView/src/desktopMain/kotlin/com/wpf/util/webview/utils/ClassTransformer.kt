//package com.wpf.util.webview.utils
//
//import javassist.ClassPool
//import javassist.CtClass
//import javassist.CtMethod
//import java.io.ByteArrayInputStream
//import java.lang.instrument.ClassFileTransformer
//import java.security.ProtectionDomain
//
//
//class ClassTransformer : ClassFileTransformer {
//
//    override fun transform(
//        loader: ClassLoader?,
//        className: String?,
//        classBeingRedefined: Class<*>?,
//        protectionDomain: ProtectionDomain?,
//        classfileBuffer: ByteArray?
//    ): ByteArray? {
//        try {
//            val ctClass: CtClass = ClassPool(true).makeClass(ByteArrayInputStream(classfileBuffer))
//            val method = ctClass.getDeclaredMethod("getFilteredHeaderFields")
//            // 注入跨域代码
//            injectCrossDomain(method)
//            val byteCode = ctClass.toBytecode()
//            ctClass.detach()
//            return byteCode
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return null
//    }
//
//    private fun injectCrossDomain(method: CtMethod) {
//        val sb = StringBuilder()
//
//        sb.append("if (this.filteredHeaders != null) {")
//        sb.append("    return this.filteredHeaders;")
//        sb.append("} else {")
//        sb.append("    java.util.HashMap var2 = new java.util.HashMap();")
//        sb.append("    java.util.Map var1;")
//        sb.append("    if (this.cachedHeaders != null) {")
//        sb.append("        var1 = this.cachedHeaders.getHeaders();")
//        sb.append("    } else {")
//        sb.append("        var1 = this.responses.getHeaders();")
//        sb.append("    }")
//        sb.append("    java.util.Iterator var3 = var1.entrySet().iterator();")
//        sb.append("    while(var3.hasNext()) {")
//        sb.append("        java.util.Map.Entry var4 = (java.util.Map.Entry)var3.next();")
//        sb.append("        String var5 = (String)var4.getKey();")
//        sb.append("        java.util.List var6 = (java.util.List)var4.getValue();")
//        sb.append("        java.util.ArrayList var7 = new java.util.ArrayList();")
//        sb.append("        java.util.Iterator var8 = var6.iterator();")
//        sb.append("        while(var8.hasNext()) {")
//        sb.append("            String var9 = (String)var8.next();")
//        sb.append("            String var10 = this.filterHeaderField(var5, var9);")
//        sb.append("            if (var10 != null) {")
//        sb.append("                var7.add(var10);")
//        sb.append("            }")
//        sb.append("        }")
//        sb.append("        if (!var7.isEmpty()) {")
//
//        // Access-Control-Allow-Origin
//
//        // Access-Control-Allow-Origin
//        sb.append("            var2.put(\"Access-Control-Allow-Origin\", java.util.Collections.singletonList(\"*\"));")
//        // Access-Control-Allow-Headers
//        // Access-Control-Allow-Headers
//        sb.append("            var2.put(\"Access-Control-Allow-Headers\", java.util.Collections.singletonList(\"*\"));")
//
//        sb.append("            var2.put(var5, java.util.Collections.unmodifiableList(var7));")
//        sb.append("        }")
//        sb.append("    }")
//        sb.append("    return this.filteredHeaders = java.util.Collections.unmodifiableMap(var2);")
//        sb.append("}")
//
//        // 重写整个方法
//
//        // 重写整个方法
//        method.setBody(sb.toString())
//    }
//}