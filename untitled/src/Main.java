import java.io.*;
import java.util.*;

/*
 * @Author：张建-27
 */
public class Main {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.print("请选择操作（输入: 压缩/解压/）：");
        String choice = input.next();
        if (choice.equals("压缩")) {
            System.out.print("请输入文件路径和文件名(压缩)：");
            String fileName = input.next();
            try {
                compressFile(fileName);
                long startTime = System.currentTimeMillis();

                //压缩前文件字节大小
                int fileAgoSize = 0;
                int fileAfterSize = 0;
                File fileAge = new File(fileName);
                if (fileAge.exists() && fileAge.isFile()) {
                    long fileSize = fileAge.length();
                    System.out.println("压缩前文件大小: " + fileSize + " 字节");
                    fileAgoSize = (int) fileSize;
                }
                String fileAfterPath = fileName.substring(0, fileName.length() - 3);
                fileAfterPath += "zj";

                //压缩后文件字节大小
                File fileAfter = new File(fileAfterPath);
                if (fileAfter.exists() && fileAfter.isFile()) {
                    long fileSize = fileAfter.length();
                    System.out.println("压缩后文件大小: " + fileSize + " 字节");
                    fileAfterSize = (int) fileSize;
                }

                //计算空间占用
                int Size = 0;
                if (fileAgoSize > fileAfterSize) {
                    Size = fileAgoSize - fileAfterSize;
                    System.out.println("压缩成功:已为你节省" + Size + "字节");
                } else if (fileAgoSize < fileAfterSize) {
                    Size = fileAfterSize - fileAgoSize;
                    System.out.println("压缩成功:已为你增加" + Size + "字节");
                } else if (fileAgoSize == fileAfterSize) {
                    System.out.println("压缩前后大小相同");
                } else {
                    System.err.println("未知");
                }
                long endTime = System.currentTimeMillis();
                System.out.println("压缩时长:"+countTime(startTime,endTime)+"秒");
            } catch (IOException e) {
                System.err.println("压缩文件时出现错误：" + e.getMessage());
            }
        } else if (choice.equals("解压")) {
            System.out.print("请输入文件路径和文件名(解压)：");
            String fileName = input.next();
            try {
                decompressFile(fileName);
                long startTime = System.currentTimeMillis();
                long endTime = System.currentTimeMillis();
                System.out.println("解压时长:"+countTime(startTime,endTime)+"秒");
                System.out.println("文件解压成功！");
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("错误：" + e.getMessage());
            }
        } else {
            System.err.println("Error:没有找到选项");
        }
    }
    //    返回程序耗时(秒)
    private static double countTime(long startTime, long endTime){
        long milliseconds= endTime-startTime;
        return (double) milliseconds / 1000;
    }

    private static final int BUFFER_SIZE = 4096;

    private record Node(byte value, int frequency, Main.Node leftChild,
                        Main.Node rightChild) implements Comparable<Node>, Serializable {

        private boolean isLeaf() {
            return leftChild == null && rightChild == null;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(frequency, other.frequency);
        }
    }


    private static byte[] readBytesFromFile(File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }

    private static Map<Byte, Integer> countFrequencies(byte[] data) {
        Map<Byte, Integer> frequencies = new HashMap<>();
        for (byte b : data) {
            frequencies.put(b, frequencies.getOrDefault(b, 0) + 1);
        }
        return frequencies;
    }

    private static Node buildHuffmanTree(Map<Byte, Integer> frequencies) {
        PriorityQueue<Node> priorityQueue = new PriorityQueue<>();
        for (Map.Entry<Byte, Integer> entry : frequencies.entrySet()) {
            priorityQueue.offer(new Node(entry.getKey(), entry.getValue(), null, null));
        }
        while (priorityQueue.size() > 1) {
            Node leftChild = priorityQueue.poll();
            Node rightChild = priorityQueue.poll();
            Node parent = new Node((byte) 0, leftChild.frequency + rightChild.frequency, leftChild, rightChild);
            priorityQueue.offer(parent);
        }
        return priorityQueue.poll();
    }

    private static void generateLookupTable(Node node, String code, Map<Byte, String> lookupTable) {
        if (node.isLeaf()) {
            lookupTable.put(node.value, code);
            return;
        }
        generateLookupTable(node.leftChild, code + "0", lookupTable);
        generateLookupTable(node.rightChild, code + "1", lookupTable);
    }

    private static byte[] compressData(Node root, byte[] data, Map<Byte, String> lookupTable) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : data) {
            stringBuilder.append(lookupTable.get(b));
        }
        String encodedData = stringBuilder.toString();
        int numPaddingBits = 8 - encodedData.length() % 8;
        numPaddingBits = numPaddingBits == 8 ? 0 : numPaddingBits;
        stringBuilder.append("0".repeat(numPaddingBits));
        stringBuilder.insert(0, String.format("%8s", Integer.toBinaryString(numPaddingBits)).replace(' ', '0'));
        String paddedData = stringBuilder.toString();
        byte[] compressedData = new byte[paddedData.length() / 8];
        for (int i = 0; i < compressedData.length; i++) {
            compressedData[i] = (byte) Integer.parseInt(paddedData.substring(i * 8, (i + 1) * 8), 2);
        }
        return compressedData;
    }

    private static Node deserializeTree(byte[] treeBytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(treeBytes)) {
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            return (Node) objectInputStream.readObject();
        }
    }

    private static byte[] decompressData(Node root, byte[] compressedData) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : compressedData) {
            stringBuilder.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        String bitString = stringBuilder.toString();
        int numPaddingBits = Integer.parseInt(bitString.substring(0, 8), 2);
        bitString = bitString.substring(8, bitString.length() - numPaddingBits);
        List<Byte> result = new ArrayList<>();
        Node currentNode = root;
        for (char c : bitString.toCharArray()) {
            if (c == '0') {
                currentNode = currentNode.leftChild;
            } else {
                currentNode = currentNode.rightChild;
            }
            if (currentNode.isLeaf()) {
                result.add(currentNode.value);
                currentNode = root;
            }
        }
        byte[] decompressedData = new byte[result.size()];
        for (int i = 0; i < decompressedData.length; i++) {
            decompressedData[i] = result.get(i);
        }
        return decompressedData;
    }

    private static void saveCompressedFile(String fileName, byte[] data, Node root) throws IOException {
        try (DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))) {
            outputStream.writeInt(getSerializedTreeSize(root));
            byte[] serializedTree = serializeTree(root);
            outputStream.write(serializedTree);
            outputStream.writeInt(data.length);
            outputStream.write(data);
        }
    }

    private static void saveDecompressedFile(String fileName, byte[] data) throws IOException {
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileName))) {
            outputStream.write(data);
        }
    }

    private static int getSerializedTreeSize(Node root) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(root);
            objectOutputStream.flush();
            return outputStream.toByteArray().length;
        }
    }

    private static byte[] serializeTree(Node root) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(root);
            objectOutputStream.flush();
            return outputStream.toByteArray();
        }
    }

    private static void compressFile(String fileName) throws IOException {
        File inputFile = new File(fileName);
        String outputFile = inputFile.getName().replaceAll("\\.txt$", "") + ".zj";
        byte[] data = readBytesFromFile(inputFile);
        Map<Byte, Integer> frequencies = countFrequencies(data);
        Node root = buildHuffmanTree(frequencies);
        Map<Byte, String> lookupTable = new HashMap<>();
        generateLookupTable(root, "", lookupTable);
        byte[] compressedData = compressData(root, data, lookupTable);
        saveCompressedFile(outputFile, compressedData, root);
    }

    private static void decompressFile(String fileName) throws IOException, ClassNotFoundException {
        File inputFile = new File(fileName);
        String outputFile = inputFile.getName().replaceAll("\\.zj$", "") + ".txt";
        try (DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)))) {
            int treeSize = inputStream.readInt();
            byte[] treeBytes = new byte[treeSize];
            inputStream.readFully(treeBytes);
            Node root = deserializeTree(treeBytes);
            int dataSize = inputStream.readInt();
            byte[] compressedData = new byte[dataSize];
            inputStream.readFully(compressedData);
            byte[] decompressedData = decompressData(root, compressedData);
            saveDecompressedFile(outputFile, decompressedData);
        }
    }
}