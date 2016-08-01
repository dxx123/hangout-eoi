package com.ctrip.ops.sysdev.filters;

import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class XML extends BaseFilter {

	public XML(Map config) {
		super(config);
	}

	@Override
	protected void prepare() {
		super.prepare();
	}

	@Override
	protected Map filter(Map event) {
		// 读字符串形成DOM树
		String src = (String) event.get((String) config.get("src"));
		try {
			Document doc = (Document) DocumentHelper.parseText(src);
			Element rootNode = doc.getRootElement();
			// 获取root节点的子标签
			List<Element> firstLayerList = rootNode.elements();
			parseXML(firstLayerList, event);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return event;
	}

	private Map parseXML(List<Element> childEleList, Map event) {
		// 判断是否有元素
		if (childEleList.size() != 0) {
			for (Element childEle : childEleList) {
				// 值为空不放入event
				if (!childEle.getText().trim().equals("")) {
					// 获取标签名和值
					event.put(childEle.getName(), childEle.getText());
				}
				// 获取子节点
				List<Element> eChildList = childEle.elements();
				parseXML(eChildList, event);
			}
		}
		postProcess(event, true);
		return event;
	}
}
