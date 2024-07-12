package com.wpf.utils.jiagu.utils.parsedex

import com.wpf.utils.jiagu.utils.parsedex.Utils.bytesToHexString
import com.wpf.utils.jiagu.utils.parsedex.struct.*


class ParseDexUtils {
    private var DEBUG = false

    private var stringIdOffset = 0
    private var stringIdsSize = 0
    private var stringIdsOffset = 0
    private var typeIdsSize = 0
    private var typeIdsOffset = 0
    private var protoIdsSize = 0
    private var protoIdsOffset = 0
    private var fieldIdsSize = 0
    private var fieldIdsOffset = 0
    private var methodIdsSize = 0
    private var methodIdsOffset = 0
    private var classIdsSize = 0
    private var classIdsOffset = 0

    private var mapListOffset = 0

    private val stringIdsList: MutableList<StringIdsItem> = ArrayList()
    private val typeIdsList: MutableList<TypeIdsItem> = ArrayList()
    private val protoIdsList: MutableList<ProtoIdsItem> = ArrayList()
    private val fieldIdsList: MutableList<FieldIdsItem> = ArrayList()
    private val methodIdsList: MutableList<MethodIdsItem> = ArrayList()
    private val classIdsList: MutableList<ClassDefItem> = ArrayList()

    private val dataItemList: MutableList<ClassDataItem> = ArrayList()

    private val directMethodCodeItemList: MutableList<CodeItem> = ArrayList()
    internal val directMethodCodeItemMap: MutableMap<String, CodeItem> = mutableMapOf()
    private val virtualMethodCodeItemList: MutableList<CodeItem> = ArrayList()
    internal val virtualMethodCodeItemMap: MutableMap<String, CodeItem> = mutableMapOf()

    private val stringList: MutableList<String> = ArrayList()

    // 这里的map用来存储code数据，因为一个ClassCode都是以class_idx为单位的，所以这里的key就是classname来存储
    private val classDataMap = HashMap<String, ClassDefItem>()

    private fun parseDexHeader(byteSrc: ByteArray) {
        val headerType = HeaderType()
        // 解析endian_tag
        var isLittleEndian = true
        var endianTagByte = Utils.copyByte(byteSrc, 40, 4)
        if (endianTagByte!![0].toInt() == 120 && endianTagByte[1].toInt() == 86 && endianTagByte[2].toInt() == 52 && endianTagByte[3].toInt() == 18) {
            isLittleEndian = false
        }
        if (isLittleEndian) {
            endianTagByte = endianTagByte.reversedArray()
        }
        headerType.endian_tag = Utils.byte2int(endianTagByte)

        // 解析魔数
        val magic = Utils.copyByte(byteSrc, 0, 8)
        headerType.magic = magic

        // 解析checksum
        var checksumByte = Utils.copyByte(byteSrc, 8, 4)
        if (isLittleEndian) {
            checksumByte = checksumByte!!.reversedArray()
        }
        headerType.checksum = Utils.byte2int(checksumByte!!)

        // 解析siganature
        val siganature = Utils.copyByte(byteSrc, 12, 20)
        headerType.siganature = siganature

        // 解析siganature
        var fileSizeByte = Utils.copyByte(byteSrc, 32, 4)
        if (isLittleEndian) {
            fileSizeByte = fileSizeByte!!.reversedArray()
        }
        headerType.file_size = Utils.byte2int(fileSizeByte!!)

        // 解析header_size
        var headerSizeByte = Utils.copyByte(byteSrc, 36, 4)
        if (isLittleEndian) {
            headerSizeByte = headerSizeByte!!.reversedArray()
        }
        headerType.header_size = Utils.byte2int(headerSizeByte!!)

        // 解析link_size
        var linkSizeByte = Utils.copyByte(byteSrc, 44, 4)
        if (isLittleEndian) {
            linkSizeByte = linkSizeByte!!.reversedArray()
        }
        headerType.link_size = Utils.byte2int(linkSizeByte!!)

        // 解析link_off
        var linkOffByte = Utils.copyByte(byteSrc, 48, 4)
        if (isLittleEndian) {
            linkOffByte = linkOffByte!!.reversedArray()
        }
        headerType.link_off = Utils.byte2int(linkOffByte!!)

        // 解析map_off
        var mapOffByte = Utils.copyByte(byteSrc, 52, 4)
        if (isLittleEndian) {
            mapOffByte = mapOffByte!!.reversedArray()
        }
        headerType.map_off = Utils.byte2int(mapOffByte!!)

        // 解析string_ids_size
        var stringIdsSizeByte = Utils.copyByte(byteSrc, 56, 4)
        if (isLittleEndian) {
            stringIdsSizeByte = stringIdsSizeByte!!.reversedArray()
        }
        headerType.string_ids_size = Utils.byte2int(stringIdsSizeByte!!)

        // 解析string_ids_off
        var stringIdsOffByte = Utils.copyByte(byteSrc, 60, 4)
        if (isLittleEndian) {
            stringIdsOffByte = stringIdsOffByte!!.reversedArray()
        }
        headerType.string_ids_off = Utils.byte2int(stringIdsOffByte!!)

        // 解析type_ids_size
        var typeIdsSizeByte = Utils.copyByte(byteSrc, 64, 4)
        if (isLittleEndian) {
            typeIdsSizeByte = typeIdsSizeByte!!.reversedArray()
        }
        headerType.type_ids_size = Utils.byte2int(typeIdsSizeByte!!)

        // 解析type_ids_off
        var typeIdsOffByte = Utils.copyByte(byteSrc, 68, 4)
        if (isLittleEndian) {
            typeIdsOffByte = typeIdsOffByte!!.reversedArray()
        }
        headerType.type_ids_off = Utils.byte2int(typeIdsOffByte!!)

        // 解析proto_ids_size
        var protoIdsSizeByte = Utils.copyByte(byteSrc, 72, 4)
        if (isLittleEndian) {
            protoIdsSizeByte = protoIdsSizeByte!!.reversedArray()
        }
        headerType.proto_ids_size = Utils.byte2int(protoIdsSizeByte!!)

        // 解析proto_ids_off
        var protoIdsOffByte = Utils.copyByte(byteSrc, 76, 4)
        if (isLittleEndian) {
            protoIdsOffByte = protoIdsOffByte!!.reversedArray()
        }
        headerType.proto_ids_off = Utils.byte2int(protoIdsOffByte!!)

        // 解析field_ids_size
        var fieldIdsSizeByte = Utils.copyByte(byteSrc, 80, 4)
        if (isLittleEndian) {
            fieldIdsSizeByte = fieldIdsSizeByte!!.reversedArray()
        }
        headerType.field_ids_size = Utils.byte2int(fieldIdsSizeByte!!)

        // 解析field_ids_off
        var fieldIdsOffByte = Utils.copyByte(byteSrc, 84, 4)
        if (isLittleEndian) {
            fieldIdsOffByte = fieldIdsOffByte!!.reversedArray()
        }
        headerType.field_ids_off = Utils.byte2int(fieldIdsOffByte!!)

        // 解析method_ids_size
        var methodIdsSizeByte = Utils.copyByte(byteSrc, 88, 4)
        if (isLittleEndian) {
            methodIdsSizeByte = methodIdsSizeByte!!.reversedArray()
        }
        headerType.method_ids_size = Utils.byte2int(methodIdsSizeByte!!)

        // 解析method_ids_off
        var methodIdsOffByte = Utils.copyByte(byteSrc, 92, 4)
        if (isLittleEndian) {
            methodIdsOffByte = methodIdsOffByte!!.reversedArray()
        }
        headerType.method_ids_off = Utils.byte2int(methodIdsOffByte!!)

        // 解析class_defs_size
        var classDefsSizeByte = Utils.copyByte(byteSrc, 96, 4)
        if (isLittleEndian) {
            classDefsSizeByte = classDefsSizeByte!!.reversedArray()
        }
        headerType.class_defs_size = Utils.byte2int(classDefsSizeByte!!)

        // 解析class_defs_off
        var classDefsOffByte = Utils.copyByte(byteSrc, 100, 4)
        if (isLittleEndian) {
            classDefsOffByte = classDefsOffByte!!.reversedArray()
        }
        headerType.class_defs_off = Utils.byte2int(classDefsOffByte!!)

        // 解析data_size
        var dataSizeByte = Utils.copyByte(byteSrc, 104, 4)
        if (isLittleEndian) {
            dataSizeByte = dataSizeByte!!.reversedArray()
        }
        headerType.data_size = Utils.byte2int(dataSizeByte!!)

        // 解析data_off
        var dataOffByte = Utils.copyByte(byteSrc, 108, 4)
        if (isLittleEndian) {
            dataOffByte = dataOffByte!!.reversedArray()
        }
        headerType.data_off = Utils.byte2int(dataOffByte!!)

        if (DEBUG) {
            println("header:$headerType")
        }

        stringIdOffset = headerType.header_size

        stringIdsSize = headerType.string_ids_size
        stringIdsOffset = headerType.string_ids_off
        typeIdsSize = headerType.type_ids_size
        typeIdsOffset = headerType.type_ids_off
        fieldIdsSize = headerType.field_ids_size
        fieldIdsOffset = headerType.field_ids_off
        protoIdsSize = headerType.proto_ids_size
        protoIdsOffset = headerType.proto_ids_off
        methodIdsSize = headerType.method_ids_size
        methodIdsOffset = headerType.method_ids_off
        classIdsSize = headerType.class_defs_size
        classIdsOffset = headerType.class_defs_off

        mapListOffset = headerType.map_off
    }

    /************************ 解析字符串 ********************************/
    private fun parseStringIds(srcByte: ByteArray) {
        val idSize = StringIdsItem.getSize()
        val countIds = stringIdsSize
        for (i in 0 until countIds) {
            stringIdsList.add(parseStringIdsItem(Utils.copyByte(srcByte, stringIdsOffset + i * idSize, idSize)))
        }
        if (DEBUG) {
            println("string size:" + stringIdsList.size)
        }
    }

    private fun parseStringList(srcByte: ByteArray) {
        // 第一个字节还是字符串的长度
        for (item in stringIdsList) {
            val str = getString(srcByte, item.string_data_off)
            if (DEBUG) {
                println("str:$str")
            }
            stringList.add(str)
        }
    }

    /*************************** 解析类型 ******************************/
    private fun parseTypeIds(srcByte: ByteArray) {
        val idSize = TypeIdsItem.getSize()
        val countIds = typeIdsSize
        for (i in 0 until countIds) {
            typeIdsList.add(parseTypeIdsItem(Utils.copyByte(srcByte, typeIdsOffset + i * idSize, idSize)))
        }

        // 这里的descriptor_idx就是解析之后的字符串中的索引值
        if (DEBUG) {
            for (item in typeIdsList) {
                println("typeStr:" + stringList[item.descriptor_idx])
            }
        }
    }

    /*************************** 解析Proto ***************************/
    private fun parseProtoIds(srcByte: ByteArray?) {
        val idSize = ProtoIdsItem.getSize()
        val countIds = protoIdsSize
        for (i in 0 until countIds) {
            protoIdsList.add(parseProtoIdsItem(Utils.copyByte(srcByte, protoIdsOffset + i * idSize, idSize)))
        }

        for (item in protoIdsList) {
            if (DEBUG) {
                println("proto:" + stringList[item.shorty_idx] + "," + stringList[item.return_type_idx])
            }
            // 有的方法没有参数，这个值就是0
            if (item.parameters_off != 0) {
                parseParameterTypeList(srcByte, item.parameters_off, item)
            }
        }
    }

    // 解析方法的所有参数类型
    private fun parseParameterTypeList(srcByte: ByteArray?, startOff: Int, item: ProtoIdsItem): ProtoIdsItem {
        // 解析size和size大小的List中的内容
        val sizeByte = Utils.copyByte(srcByte, startOff, 4)
        val size = Utils.byte2int(sizeByte!!)
        val parametersList: MutableList<String> = ArrayList()
        val typeList: MutableList<Short> = ArrayList(size)
        for (i in 0 until size) {
            val typeByte = Utils.copyByte(srcByte, startOff + 4 + 2 * i, 2)
            typeList.add(Utils.byte2Short(typeByte!!))
        }
        if (DEBUG) {
            println("param count:$size")
        }
        for (i in typeList.indices) {
            if (DEBUG) {
                println("type:" + stringList[typeList[i].toInt()])
            }
            val index = typeIdsList[typeList[i].toInt()].descriptor_idx
            parametersList.add(stringList[index])
        }

        item.parameterCount = size
        item.parametersList = parametersList

        return item
    }

    /*************************** 解析字段 ****************************/
    private fun parseFieldIds(srcByte: ByteArray?) {
        val idSize = FieldIdsItem.getSize()
        val countIds = fieldIdsSize
        for (i in 0 until countIds) {
            fieldIdsList.add(parseFieldIdsItem(Utils.copyByte(srcByte, fieldIdsOffset + i * idSize, idSize)))
        }

        if (DEBUG) {
            for (item in fieldIdsList) {
                val classIndex = typeIdsList[item.class_idx.toInt()].descriptor_idx
                val typeIndex = typeIdsList[item.type_idx.toInt()].descriptor_idx
                println("class:" + stringList[classIndex] + ",name:" + stringList[item.name_idx] + ",type:" + stringList[typeIndex])
            }
        }
    }

    /*************************** 解析方法 *****************************/
    private fun parseMethodIds(srcByte: ByteArray) {
        val idSize = MethodIdsItem.getSize()
        val countIds = methodIdsSize
        for (i in 0 until countIds) {
            methodIdsList.add(parseMethodIdsItem(Utils.copyByte(srcByte, methodIdsOffset + i * idSize, idSize)))
        }

        if (DEBUG) {
            for (item in methodIdsList) {
                val classIndex = typeIdsList[item.class_idx.toInt()].descriptor_idx
                val returnIndex = protoIdsList[item.proto_idx.toInt()].return_type_idx
                val returnTypeStr = stringList[typeIdsList[returnIndex].descriptor_idx]
                val shortIndex = protoIdsList[item.proto_idx.toInt()].shorty_idx
                val shortStr = stringList[shortIndex]
                val paramList = protoIdsList[item.proto_idx.toInt()].parametersList
                val parameters = StringBuilder()
                parameters.append("$returnTypeStr(")
                for (str in paramList) {
                    parameters.append("$str,")
                }
                parameters.append(")$shortStr")
                println("class:" + stringList[classIndex] + ",name:" + stringList[item.name_idx] + ",proto:" + parameters)
            }
        }
    }

    /**************************** 解析类 *****************************/
    private fun parseClassIds(srcByte: ByteArray) {
        if (DEBUG) {
            println("classIdsOffset:" + bytesToHexString(Utils.int2Byte(classIdsOffset)))
            println("classIds:$classIdsSize")
        }
        val idSize = ClassDefItem.getSize()
        val countIds = classIdsSize
        for (i in 0 until countIds) {
            classIdsList.add(parseClassDefItem(Utils.copyByte(srcByte, classIdsOffset + i * idSize, idSize)))
        }
        for (item in classIdsList) {
            val classIdx = item.class_idx
            if (DEBUG) {
                println("item:$item")
                val typeItem = typeIdsList[classIdx]
                println("classIdx:" + stringList[typeItem.descriptor_idx])
                val superClassIdx = item.superclass_idx
                val superTypeItem = typeIdsList[superClassIdx]
                println("superitem:" + stringList[superTypeItem.descriptor_idx])
                val sourceIdx = item.source_file_idx
                val sourceFile = stringList[sourceIdx]
                println("sourceFile:$sourceFile")
            }
            classDataMap[classIdx.toString()] = item
        }
    }

    /*************************** 解析ClassData ***************************/
    private fun parseClassData(srcByte: ByteArray) {
        for (key in classDataMap.keys.sortedBy { it.toInt() }) {
            val dataOffset = classDataMap[key]!!.class_data_off
            if (DEBUG) {
                println("data offset:" + bytesToHexString(Utils.int2Byte(dataOffset)))
            }
            val item = parseClassDataItem(srcByte, dataOffset, key)
            dataItemList.add(item)
            if (DEBUG) {
                println("class item:$item")
            }
        }
    }

    private fun parseClassDataItem(srcByte: ByteArray, offset: Int, classIdx: String? = null): ClassDataItem {
        var offsetTemp = offset
        val item = ClassDataItem(classIdx)
        if (offsetTemp == 0) {
            return item
        }
        for (i in 0..3) {
            val byteAry = Utils.readUnsignedLeb128(srcByte, offsetTemp)
            offsetTemp += byteAry.size
            when (i) {
                0 -> {
                    item.static_fields_size = Utils.decodeULeb128(byteAry)
                }

                1 -> {
                    item.instance_fields_size = Utils.decodeULeb128(byteAry)
                }

                2 -> {
                    item.direct_methods_size = Utils.decodeULeb128(byteAry)
                }

                3 -> {
                    item.virtual_methods_size = Utils.decodeULeb128(byteAry)
                }
            }
        }


        // 解析static_fields数组
        val staticFieldAry = arrayOfNulls<EncodedField>(item.static_fields_size)
        for (i in 0 until item.static_fields_size) {
            /**
             * public int filed_idx_diff;
             * public int access_flags;
             */
            val staticField = EncodedField()
            staticField.filed_idx_diff = Utils.readUnsignedLeb128(srcByte, offsetTemp)
            offsetTemp += staticField.filed_idx_diff.size
            staticField.access_flags = Utils.readUnsignedLeb128(srcByte, offsetTemp)
            offsetTemp += staticField.access_flags.size
            staticFieldAry[i] = staticField
        }

        // 解析instance_fields数组
        val instanceFieldAry = arrayOfNulls<EncodedField>(item.instance_fields_size)
        for (i in 0 until item.instance_fields_size) {
            /**
             * public int filed_idx_diff;
             * public int access_flags;
             */
            val instanceField = EncodedField()
            instanceField.filed_idx_diff = Utils.readUnsignedLeb128(srcByte, offsetTemp)
            offsetTemp += instanceField.filed_idx_diff.size
            instanceField.access_flags = Utils.readUnsignedLeb128(srcByte, offsetTemp)
            offsetTemp += instanceField.access_flags.size
            instanceFieldAry[i] = instanceField
        }

        // 解析static_methods 数组
        val staticMethodsAry = arrayOfNulls<EncodedMethod>(item.direct_methods_size)
        for (i in 0 until item.direct_methods_size) {
            /**
             * public byte[] method_idx_diff;
             * public byte[] access_flags;
             * public byte[] code_off;
             */
            val directMethod = EncodedMethod(classIdx)
            directMethod.method_idx_diff = Utils.readUnsignedLeb128(srcByte, offsetTemp)
            offsetTemp += directMethod.method_idx_diff.size
            directMethod.access_flags = Utils.readUnsignedLeb128(srcByte, offsetTemp)
            offsetTemp += directMethod.access_flags.size
            directMethod.code_off = Utils.readUnsignedLeb128(srcByte, offsetTemp)
            offsetTemp += directMethod.code_off.size
            staticMethodsAry[i] = directMethod
        }

        // 解析virtual_methods 数组
        val instanceMethodsAry = arrayOfNulls<EncodedMethod>(item.virtual_methods_size)
        for (i in 0 until item.virtual_methods_size) {
            /**
             * public byte[] method_idx_diff;
             * public byte[] access_flags;
             * public byte[] code_off;
             */
            val instanceMethod = EncodedMethod(classIdx)
            instanceMethod.method_idx_diff = Utils.readUnsignedLeb128(srcByte, offsetTemp)
            offsetTemp += instanceMethod.method_idx_diff.size
            instanceMethod.access_flags = Utils.readUnsignedLeb128(srcByte, offsetTemp)
            offsetTemp += instanceMethod.access_flags.size
            instanceMethod.code_off = Utils.readUnsignedLeb128(srcByte, offsetTemp)
            offsetTemp += instanceMethod.code_off.size
            instanceMethodsAry[i] = instanceMethod
        }

        item.static_fields = staticFieldAry.requireNoNulls()
        item.instance_fields = instanceFieldAry.requireNoNulls()
        item.direct_methods = staticMethodsAry.requireNoNulls()
        item.virtual_methods = instanceMethodsAry.requireNoNulls()

        return item
    }

    /*************************** 解析代码内容 ***************************/
    private fun parseCode(srcByte: ByteArray) {
        for (item in dataItemList) {
            var premid = 0
            for (item1 in item.direct_methods) {
                if (item1.code_off.isNotEmpty() && item1.code_off.any { it.toInt() == 0 }) continue
                val offset = Utils.decodeULeb128(item1.code_off)
                val items = parseCodeItem(srcByte, offset, item1.classIdx)
//                directMethodCodeItemList.add(items)
                val index = Utils.decodeULeb128(item1.method_idx_diff) + premid
                premid = index
                val methodItem = methodIdsList[index]
                directMethodCodeItemMap[getMethodSignStr(methodItem)] = items
                if (DEBUG) {
                    println("direct method item:$items")
                }
            }
            premid = 0
            for (item1 in item.virtual_methods) {
                if (item1.code_off.isNotEmpty() && item1.code_off.any { it.toInt() == 0 }) continue
                val offset = Utils.decodeULeb128(item1.code_off)
                val items = parseCodeItem(srcByte, offset, item1.classIdx)
//                virtualMethodCodeItemList.add(items)
                val index = Utils.decodeULeb128(item1.method_idx_diff) + premid
                premid = index
                val methodItem = methodIdsList[index]
                virtualMethodCodeItemMap[getMethodSignStr(methodItem)] = items
                if (DEBUG) {
                    println("virtual method item:$items")
                }
            }
        }
    }

    //得到方法的唯一签名
    private fun getMethodSignStr(methodItem: MethodIdsItem): String {
        val classIndex = typeIdsList[methodItem.class_idx.toInt()].descriptor_idx
        //获得类名
        val className = stringList[classIndex]
        //获得方法名称
        val methodName = stringList[methodItem.name_idx]
        //获得方法签名
        val protoIdsItem = protoIdsList[methodItem.proto_idx.toInt()]
        val protoName = stringList[protoIdsItem.shorty_idx]
        val parametersList = protoIdsItem.parametersList.joinToString()
        //返回值
        val returnIndex = typeIdsList[protoIdsItem.return_type_idx].descriptor_idx
        val returnName = stringList[returnIndex]

        val sinName = "$className$methodName($parametersList)#$returnName()$protoName"
        if (DEBUG) {
            println("Shark:$sinName")
        }
        return sinName
    }

    private fun parseCodeItem(srcByte: ByteArray, offset: Int, classIdx: String? = null): CodeItem {
        val item = CodeItem(classIdx)
        if (offset == 0) {
            return item
        }

        /**
         * public short registers_size;
         * public short ins_size;
         * public short outs_size;
         * public short tries_size;
         * public int debug_info_off;
         * public int insns_size;
         * public short[] insns;
         */
        val regSizeByte = Utils.copyByte(srcByte, offset, 2)
        item.registers_size = Utils.byte2Short(regSizeByte!!)

        val insSizeByte = Utils.copyByte(srcByte, offset + 2, 2)
        item.ins_size = Utils.byte2Short(insSizeByte!!)

        val outsSizeByte = Utils.copyByte(srcByte, offset + 4, 2)
        item.outs_size = Utils.byte2Short(outsSizeByte!!)

        val triesSizeByte = Utils.copyByte(srcByte, offset + 6, 2)
        item.tries_size = Utils.byte2Short(triesSizeByte!!)

        val debugInfoByte = Utils.copyByte(srcByte, offset + 8, 4)
        item.debug_info_off = Utils.byte2int(debugInfoByte!!)

        val insnsSizeByte = Utils.copyByte(srcByte, offset + 12, 4)
        item.insns_size = Utils.byte2int(insnsSizeByte!!)

        //赋值指令的偏移
        item.insnsOffset = offset + 16

        val insnsAry = ShortArray(item.insns_size)
        val aryOffset = offset + 16
        for (i in 0 until item.insns_size) {
            val insnsByte = Utils.copyByte(srcByte, aryOffset + i * 2, 2)
            insnsAry[i] = Utils.byte2Short(insnsByte!!)
        }
        item.insns = insnsAry

        return item
    }

    private fun parseMapItemList(srcByte: ByteArray?) {
        val mapList = MapList()
        val sizeByte = Utils.copyByte(srcByte, mapListOffset, 4)
        val size = Utils.byte2int(sizeByte!!)
        for (i in 0 until size) {
            mapList.map_item.add(
                parseMapItem(
                    Utils.copyByte(
                        srcByte,
                        mapListOffset + 4 + i * MapItem.getSize(),
                        MapItem.getSize()
                    )
                )
            )
        }
    }

    private fun parseStringIdsItem(srcByte: ByteArray?): StringIdsItem {
        val item = StringIdsItem()
        val idsByte = Utils.copyByte(srcByte, 0, 4)
        item.string_data_off = Utils.byte2int(idsByte!!)
        return item
    }

    private fun parseTypeIdsItem(srcByte: ByteArray?): TypeIdsItem {
        val item = TypeIdsItem()
        val descriptorIdxByte = Utils.copyByte(srcByte, 0, 4)
        item.descriptor_idx = Utils.byte2int(descriptorIdxByte!!)
        return item
    }

    private fun parseProtoIdsItem(srcByte: ByteArray?): ProtoIdsItem {
        val item = ProtoIdsItem()
        val shortyIdxByte = Utils.copyByte(srcByte, 0, 4)
        item.shorty_idx = Utils.byte2int(shortyIdxByte!!)
        val returnTypeIdxByte = Utils.copyByte(srcByte, 4, 8)
        item.return_type_idx = Utils.byte2int(returnTypeIdxByte!!)
        val parametersOffByte = Utils.copyByte(srcByte, 8, 4)
        item.parameters_off = Utils.byte2int(parametersOffByte!!)
        return item
    }


    private fun parseFieldIdsItem(srcByte: ByteArray?): FieldIdsItem {
        val item = FieldIdsItem()
        val classIdxByte = Utils.copyByte(srcByte, 0, 2)
        item.class_idx = Utils.byte2Short(classIdxByte!!)
        val typeIdxByte = Utils.copyByte(srcByte, 2, 2)
        item.type_idx = Utils.byte2Short(typeIdxByte!!)
        val nameIdxByte = Utils.copyByte(srcByte, 4, 4)
        item.name_idx = Utils.byte2int(nameIdxByte!!)
        return item
    }

    private fun parseMethodIdsItem(srcByte: ByteArray?): MethodIdsItem {
        val item = MethodIdsItem()
        val classIdxByte = Utils.copyByte(srcByte, 0, 2)
        item.class_idx = Utils.byte2Short(classIdxByte!!)
        val protoIdxByte = Utils.copyByte(srcByte, 2, 2)
        item.proto_idx = Utils.byte2Short(protoIdxByte!!)
        val nameIdxByte = Utils.copyByte(srcByte, 4, 4)
        item.name_idx = Utils.byte2int(nameIdxByte!!)
        return item
    }

    private fun parseClassDefItem(srcByte: ByteArray?): ClassDefItem {
        val item = ClassDefItem()
        val classIdxByte = Utils.copyByte(srcByte, 0, 4)
        item.class_idx = Utils.byte2int(classIdxByte!!)
        val accessFlagsByte = Utils.copyByte(srcByte, 4, 4)
        item.access_flags = Utils.byte2int(accessFlagsByte!!)
        val superClassIdxByte = Utils.copyByte(srcByte, 8, 4)
        item.superclass_idx = Utils.byte2int(superClassIdxByte!!)
        // 这里如果class没有interfaces的话，这里就为0
        val interfacesOffByte = Utils.copyByte(srcByte, 12, 4)
        item.iterfaces_off = Utils.byte2int(interfacesOffByte!!)
        // 如果此项信息缺失，值为0xFFFFFF
        val sourceFileIdxByte = Utils.copyByte(srcByte, 16, 4)
        item.source_file_idx = Utils.byte2int(sourceFileIdxByte!!)

        val annotationsOffByte = Utils.copyByte(srcByte, 20, 4)
        item.annotations_off = Utils.byte2int(annotationsOffByte!!)

        val classDataOffByte = Utils.copyByte(srcByte, 24, 4)
        item.class_data_off = Utils.byte2int(classDataOffByte!!)

        val staticValueOffByte = Utils.copyByte(srcByte, 28, 4)
        item.static_value_off = Utils.byte2int(staticValueOffByte!!)
        return item
    }

    private fun parseMapItem(srcByte: ByteArray?): MapItem {
        val item = MapItem()
        val typeByte = Utils.copyByte(srcByte, 0, 2)
        item.type = Utils.byte2Short(typeByte!!)
        val unuseByte = Utils.copyByte(srcByte, 2, 2)
        item.unuse = Utils.byte2Short(unuseByte!!)
        val sizeByte = Utils.copyByte(srcByte, 4, 4)
        item.size = Utils.byte2int(sizeByte!!)
        val offsetByte = Utils.copyByte(srcByte, 8, 4)
        item.offset = Utils.byte2int(offsetByte!!)
        return item
    }

    /**
     * 这里是解析一个字符串 有两种方式 1、第一个字节就是字符串的长度 2、每个字符串的结束符是00
     *
     * @param srcByte
     * @param startOff
     * @return
     */
    private fun getString(srcByte: ByteArray, startOff: Int): String {
        val size = srcByte[startOff]
        val strByte = Utils.copyByte(srcByte, startOff + 1, size.toInt())
        var result = ""
        try {
            if (strByte != null) {
                result = String(strByte, charset("UTF-8"))
            }
        } catch (ignore: Exception) {
        }
        return result
    }

    fun parseAll(srcByte: ByteArray) {
        parseDexHeader(srcByte)
        parseStringIds(srcByte)
        parseStringList(srcByte)
        parseTypeIds(srcByte)
        parseProtoIds(srcByte)
        parseFieldIds(srcByte)
        parseMethodIds(srcByte)
        parseClassIds(srcByte)
        parseMapItemList(srcByte)
        parseClassData(srcByte)
        parseCode(srcByte)
    }
}
