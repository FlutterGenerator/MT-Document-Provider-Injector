
/*
 * MT-Document-Provider-Injector 
 * Copyright 2024, developer-krushna
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of developer-krushna nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


 *     Please contact Krushna by email mt.modder.hub@gmail.com if you need
 *     additional information or have any questions
 */
package mt.document.provider.injector.util;

import java.util.*;
import org.jetbrains.annotations.*;
import pxb.android.axml.*;
import pxb.android.axml.Axml.*;
import pxb.android.axml.Axml.Node.*;

/* 
 Author @developer-krushna
 */
 
public class ManifestEditor {
	
	private static final String NS_ANDROID = "http://schemas.android.com/apk/res/android";
	
	@Contract("null, _, _ -> null")
	private static Node findNodeByName(Axml axml, String name, String... attributes) {
		return axml != null ? findNodeByName(axml.firsts, name, attributes) : null;
	}
	
	@Contract("null, _, _ -> null")
	private static Node findNodeByName(Node node, String name, String... attributes) {
		return node != null ? findNodeByName(node.children, name, attributes) : null;
	}
	
	@Contract("null, _, _ -> null")
	private static Node findNodeByName(List<Node> nodes, String name, String... attributes) {
		if (!(nodes == null || nodes.isEmpty())) {
			int pos = name.indexOf(".");
			if (pos != -1) {
				String parentName = name.substring(0, pos);
				String childName = name.substring(pos + 1);
				for (Node node : nodes) {
					if (parentName.equals(node.name)) {
						Node childNode = findNodeByName(node, childName, attributes);
						if (childNode != null) {
							return childNode;
						}
					}
				}
				return null;
			}
			for (Node node2 : nodes) {
				if (name.equals(node2.name)) {
					if (attributes.length > 0) {
						boolean b = false;
						int n = 0;
						while (n < attributes.length) {
							for (Attr attr : node2.attrs) {
								if (attributes[n].equals(attr.name) && attributes[n + 1].equals(attr.value)) {
									b = true;
									break;
								}
							}
							n += 2;
						}
					}
					return node2;
				}
			}
		}
		return null;
	}
	
	@Nullable
	public static String getAttributeValue(@NotNull Node node, String name) {
		for (Attr attr : node.attrs) {
			if (attr.name.equals(name)) {
				if (attr.value != null) {
					return attr.value.toString();
				}
				return null;
			}
		}
		return null;
	}
	
	public static void addDocumentProvider(Axml axml) {
		Node applicationNode = findNodeByName(axml, "manifest.application", new String[0]);
		if (applicationNode != null) {
			/* Insert Activity Node */
			Node activityNode = new Node();
			activityNode.name = "activity";
			activityNode.attr(NS_ANDROID, "name", android.R.attr.name, 3, "bin.mt.file.content.MTDataFilesWakeUpActivity");
			activityNode.attr(NS_ANDROID, "exported", android.R.attr.exported, 3, "true");
			activityNode.attr(NS_ANDROID, "taskAffinity", android.R.attr.taskAffinity, 3, getPackageName(axml) + ".MTDataFilesWakeUp");
			activityNode.attr(NS_ANDROID, "excludeFromRecents", android.R.attr.excludeFromRecents, 3, "true");
			activityNode.attr(NS_ANDROID, "noHistory", android.R.attr.noHistory, 3, "true");
			applicationNode.children.add(activityNode);
			
			/* Insert Provider Node */
			Node providerNode = new Node();
			providerNode.name = "provider";
			providerNode.attr(NS_ANDROID, "name", android.R.attr.name, 3, "bin.mt.file.content.MTDataFilesProvider");
			providerNode.attr(NS_ANDROID, "permission", android.R.attr.permission, 3, "android.permission.MANAGE_DOCUMENTS");
			providerNode.attr(NS_ANDROID, "exported", android.R.attr.exported, 3, "true");
			providerNode.attr(NS_ANDROID, "authorities", android.R.attr.authorities, 3, getPackageName(axml) + ".MTDataFilesProvider");
			providerNode.attr(NS_ANDROID, "grantUriPermissions", android.R.attr.grantUriPermissions, 3, "true");
			
			/* Insert intentFilter node inside Provider node */
			Node intentFilterNode = new Node();
			intentFilterNode.name = "intent-filter";
			Node actionNode = new Node();
			actionNode.name = "action";
			actionNode.attr(NS_ANDROID, "name", android.R.attr.name, 3, "android.content.action.DOCUMENTS_PROVIDER");
			intentFilterNode.children.add(actionNode);
			providerNode.children.add(intentFilterNode);
			
			applicationNode.children.add(providerNode);
		}
	}
	
	/* Get Package name */
	public static String getPackageName(Axml axml) {
		Node manifestNode = findNodeByName(axml, "manifest", new String[0]);
		if (manifestNode != null) {
			return getAttributeValue(manifestNode, "package");
		}
		return null;
	}
	
	
	
	@NotNull
	@Contract("_ -> new")
	public static AxmlVisitor getNodeVisitor(NodeVisitor original) {
		return new AxmlVisitor(original) {
			public NodeVisitor child(String ns, String name) {
				return new NodeVisitor(super.child(ns, name)) {
					public NodeVisitor child(String ns, String name) {
						return name.equals("application") ? new NodeVisitor(super.child(ns, name)) {
							public NodeVisitor child(String ns, String name) {
								return name.equals("provider") ? new NodeVisitor(super.child(ns, name)) {
									public void attr(String ns, String name, int resourceId, int type, Object obj) {
										super.attr(ns, name, resourceId, type, obj);
									}
								} : super.child(ns, name);
							}
						} : super.child(ns, name);
					}
				};
			}
		};
	}
}
