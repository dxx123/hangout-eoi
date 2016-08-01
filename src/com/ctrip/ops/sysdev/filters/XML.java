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
		// ���ַ����γ�DOM��
		String src = (String) event.get((String) config.get("src"));
		try {
			Document doc = (Document) DocumentHelper.parseText(src);
			Element rootNode = doc.getRootElement();
			// ��ȡroot�ڵ���ӱ�ǩ
			List<Element> firstLayerList = rootNode.elements();
			parseXML(firstLayerList, event);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return event;
	}

	private Map parseXML(List<Element> childEleList, Map event) {
		// �ж��Ƿ���Ԫ��
		if (childEleList.size() != 0) {
			for (Element childEle : childEleList) {
				// ֵΪ�ղ�����event
				if (!childEle.getText().trim().equals("")) {
					// ��ȡ��ǩ����ֵ
					event.put(childEle.getName(), childEle.getText());
				}
				// ��ȡ�ӽڵ�
				List<Element> eChildList = childEle.elements();
				parseXML(eChildList, event);
			}
		}
		postProcess(event, true);
		return event;
	}
}
