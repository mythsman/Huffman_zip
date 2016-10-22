import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;


//霍夫曼树的节点
class Node {

	public Node left, right;
	public int source;
	public int weight;
	public String dest;

	public Node(int sour, int wei) {
		source = sour;
		weight = wei;
		left=right=null;
		dest="";
	}
}


//写入文件的头信息
class Header implements Serializable {

	private static final long serialVersionUID = 1L;
	public String[] mp;

	public Header(String[] m) {
		mp = m;

	}
}


/**
 * 压缩解压的主类
 * @author Myths
 *
 */
public class Huffman {

	public String[] mp;
	public int[] cnt;
	public String path;

	public Huffman(String path){
		this.path = path;
		mp = new String[256];
		cnt = new int[256];
	}
	
	//封装了解压的方法
	public void unzip() throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream ins = new ObjectInputStream(new FileInputStream(new File(path)));
		FileOutputStream writer = new FileOutputStream(new File(path.substring(0, path.length() - 5)));
		Header zipFile = (Header) ins.readObject();

		Map<String, Character> mp = new HashMap<String, Character>();
		for (int i = 0; i < 256; i++) {
			if (zipFile.mp[i] != null) {
				mp.put(zipFile.mp[i], (char) i);
			}
		}

		String buff = "";
		byte[] bf = null;
		while (ins.available() >= 4) {

			if (ins.available() == 4) {
				bf = new byte[4];
				for (int i = 0; i < 4; i++)
					bf[i] = ins.readByte();
				if (ins.available() == 0) {
					break;
				}
				for (int j = 0; j < 4; j++) {
					buff += b2s(bf[j]);
					while (buff.length() > 256) {
						String s = "";
						int cnt = 0;
						for (int i = 0; i < buff.length(); i++) {
							s += buff.charAt(i);
							if (mp.containsKey(s)) {
								writer.write(mp.get(s));
								
								cnt += s.length();
								s = "";
								break;
							}
						}
						writer.flush();
						buff = buff.substring(cnt, buff.length());
					}
				}
			}
			byte c = ins.readByte();
			buff += b2s(c);
			while (buff.length() > 256) {
				String s = "";
				int cnt = 0;
				for (int i = 0; i < buff.length(); i++) {
					s += buff.charAt(i);
					if (mp.containsKey(s)) {
						writer.write(mp.get(s));
						cnt += s.length();
						s = "";
						break;
					}
				}
				writer.flush();
				buff = buff.substring(cnt, buff.length());
			}

		}

		for (int i = 0; i < 4; i++) {
			if (bf[i] == 0) {
				buff += "0";
			} else if (bf[i] == 1) {
				buff += "1";
			}
		}

		String s = "";
		for (int i = 0; i < buff.length(); i++) {
			s += buff.charAt(i);
			if (mp.containsKey(s)) {
				writer.write(mp.get(s));
				s = "";
			}
		}
		writer.flush();
		writer.close();
		ins.close();
	}

	
	//封装了压缩的方法
	public void zip() throws IOException {
		readFrequency();
		huffmanEncrypt();
		FileInputStream ins = new FileInputStream(new File(path));
		Header zipFile = new Header(mp);
		ObjectOutputStream ous = new ObjectOutputStream(new FileOutputStream(path + ".huff"));
		ous.writeObject(zipFile);

		String buff = "";
		int c;
		while ((c = ins.read()) != -1) {
			buff += mp[c];
			while (buff.length() >= 8) {
				ous.writeByte((byte) (s2b(buff.substring(0, 8))));
				buff = buff.substring(8, buff.length());
			}
		}

		for (int i = 0; i < 4; i++) {
			if (i < buff.length()) {
				ous.writeByte(buff.charAt(i) - '0');
			} else {
				ous.writeByte(255);
			}
		}

		ous.flush();
		ous.close();
		ins.close();
	}

	//字节转二进制字符串
	public String b2s(byte c) {
		int cc = (c + 256) % 256;
		String s = "";
		while (cc > 0) {
			if (cc % 2 == 1) {
				s += "1";
			} else {
				s += "0";
			}
			cc /= 2;
		}
		while (s.length() < 8) {
			s += "0";
		}
		return s;
	}

	
	//二进制字符串转字节
	public byte s2b(String s) {
		byte c = 0;
		for (int i = 7; i >= 0; i--) {
			c *= 2;
			if (s.charAt(i) == '1') {
				c += 1;
			}

		}
		return c;
	}

	// 读取文件，并获得每个字符的频数
	public void readFrequency() throws IOException { 

		File file = new File(path);
		FileInputStream ins=new FileInputStream(file);
		int c;
		while ((c = ins.read()) != -1) {
			cnt[c] += 1;	
		}
		ins.close();
	}

	// 读取频数，返回Huffman映射表
	public void huffmanEncrypt() { 

		PriorityQueue<Node> pq = new PriorityQueue<Node>(256, new Comparator<Node>() {

			@Override
			public int compare(Node o1, Node o2) {

				return o1.weight - o2.weight;
			}

		});
		int times = 0;
		for (int i = 0; i < 256; i++) {
			if (cnt[i] > 0.5) {
				pq.add(new Node(i, cnt[i]));
				times++;
			}
		}

		for (int i = 0; i < times - 1; i++) {
			Node nodeFir, nodeSec;
			nodeFir = pq.poll();
			nodeSec = pq.poll();
			Node newNode = new Node(-1, nodeSec.weight + nodeFir.weight);
			newNode.left = nodeSec;
			newNode.right = nodeFir;
			pq.add(newNode);
		}

		Node root = pq.poll();
		Queue<Node> q = new LinkedBlockingQueue<Node>();
		q.add(root);
		while (!q.isEmpty()) {
			Node cur = q.poll(); // bfs遍历
			if (cur.source == -1) { // 非叶子节点
				if (cur.left != null) {
					cur.left.dest = cur.dest + "1";
					q.add(cur.left);
				}
				if (cur.right != null) {
					cur.right.dest = cur.dest + "0";
					q.add(cur.right);
				}
			} else { // 叶子节点
				mp[cur.source] = cur.dest;
			}
		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {

		 Huffman huff = new	 Huffman("C:\\Users\\Administrator\\Desktop\\in.txt.huff");

		 huff.unzip();

		//Huffman huff = new Huffman("C:\\Users\\Administrator\\Desktop\\in.txt");

		//huff.zip();
	}
}
