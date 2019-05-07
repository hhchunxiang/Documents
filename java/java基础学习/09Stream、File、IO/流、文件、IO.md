#Stream、File、IO
##缓存流
用字节流和字符流访问硬盘的弊端：每读/写一个字节都要访问硬盘。
**缓存流**在写入数据的时候，会先把数据写入到缓存区，直到缓存区达到一定的量，才把这些数据，一起写入到硬盘中去。按照这种操作模式，就不会像字节流，字符流那样每写一个字节都访问硬盘，从而减少了IO操作。

注意：
* 在不指定自动flush的情况下，使用缓存流读数据时，只有当缓存区数据读满之后才会将数据一次性传递给程序；同理，写数据时，也只有当缓存区数据写满之后，才将数据一次性传递给外部存储介质。
* 可以调用flush()牵制将当前的数据写入目标区域。
* 关闭缓存流操作（close()），不论缓存区是否读/写满，数据将被强制读写到目标中（类似调用了一次flush()）。
* 数据还在缓存区时，若发生掉电，缓存区的数据可能会发生丢失。
```java
public static void FileWriteByBuff() {
		File f = new File("D:\\LOLFolder\\encoding\\Bufftest.txt");
		try(FileWriter fw = new FileWriter(f);
			PrintWriter pw = new PrintWriter(fw)) {
			if(!f.exists()) {
				f.createNewFile();
			}
			
			FileWriter fw = new FileWriter(f);
			PrintWriter pw = new PrintWriter(fw);
			
			pw.println("Hello World!");
			pw.println("I'm writen by hcx.");
			pw.println("!");
			
		} catch (IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
```
##数据流
> https://www.cnblogs.com/alsf/p/7226027.html?utm_source=itdadao&utm_medium=referral


数据输出流：DataOutputStream
数据输入流：DataInputStream

这两个数据操作于平台无关。
通常按照一定格式将输入输出，再按照一定格式将数据输入。
要想使用数据输出流和输入流，则肯定要用户指定数据的保存格式。必须按指定的格式保存数据，才可以将数据输入流将数据读取进来。

##对象流
对象的输入输出流的作用： 用于写入对象 的信息和读取对象的信息。 使得对象持久化。
> https://www.cnblogs.com/bigerf/p/6143911.html

##System.in

> https://www.cnblogs.com/Cherrylalala/p/6677673.html
