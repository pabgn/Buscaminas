/**
 * Created by Pablo García-Nieto Rodríguez on 27/02/14.
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.table.DefaultTableModel;
import java.util.Random;
import com.clwillingham.socket.io.*;
import org.json.*;
import javax.swing.border.*;
import java.awt.Color;

public class Game extends JPanel {
    //Creamos el Panel extendiendo a JPanel (Swing)
    private static int[][] tablero;
    private static JFrame frame;
    private static boolean alive;
    private static int numBombs;
    private static int[][] tableroDiscovered;
    private static int numDiscovered;
    private static DefaultTableModel tableModel;
    private static DefaultTableModel pointsModel;
    private static JOptionPane optionPane;
    private static JDialog dialog;
    private static boolean playing=false;
    private static IOSocket socket;
    private static String partner;
    static int[] puntos = new int[2];
    static int[] remaining = new int[2];
    private static int[][] marked;
    private static int[][] markedAuthor;
    private static int numMarked;
    private static boolean drawn;
    private static boolean reconnect=false;
    private static JPanel panel2 = new JPanel();
    private static JLabel textLog = new JLabel();
    private static JTextArea tx = new JTextArea(15, 1);
    private static JLabel perc = new JLabel();

    public Game() throws IOException {
        //Generamos la tabla de puntuacion
        JPanel p = new JPanel();
        Container c = frame.getContentPane();
        c.setLayout(new GridLayout(1,2));
        perc.setMinimumSize(new Dimension(150, 100));
        perc.setMaximumSize(new Dimension(150, 100));
        perc.setAlignmentX(Component.CENTER_ALIGNMENT);
        perc.setText("Descubierto: 0 %");
        pointsModel =  new DefaultTableModel()
        {
            @Override

            public boolean isCellEditable(int row, int col) {
                return false;

            }
        };
        final JTable  pointsTable = new JTable(pointsModel);
        pointsModel.addColumn("Jugador");
        pointsModel.addColumn("Puntos");
        pointsModel.addRow(new Object[]{"Yo", "0"});
        pointsModel.addRow(new Object[]{"Jugador 2", "0"});
        pointsTable.setPreferredScrollableViewportSize(new Dimension(500, 500));
        pointsTable.setFillsViewportHeight(true);
        pointsTable.setRowHeight(100);
        pointsTable.getTableHeader().setReorderingAllowed(false);
        p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
        p.add(perc);
        p.add(pointsTable);
        JButton b = new JButton("Reiniciar");
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(b);
        tx.setMaximumSize(new Dimension(200, 400));
        tx.setMinimumSize(new Dimension(200, 400));
        tx.setEditable(false);
        p.add(tx);
        add(p);
        //Generamos la tabla del juego
        tableModel = new DefaultTableModel()
        {
            @Override
            public Class getColumnClass(int column)
            {
                //Nos aseguramos que la respuesta a las celdas sea siempre un tipo ImageIcon
                return ImageIcon.class;
            }
            public boolean isCellEditable(int row, int col) {
                return false;

            }
        };
        //Anadimos las columnas
        for(int x=0;x<10;x++){
        tableModel.addColumn(""+x);
        }
        // Ajustes de tamaños y colores
        final JTable  table = new JTable(tableModel);
        table.setPreferredScrollableViewportSize(new Dimension(500, 500));
        table.setFillsViewportHeight(true);
        table.setRowHeight(50);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionBackground(Color.white);
        table.setTableHeader(null);
        //Interceptamos el doble click en una celda
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                    JTable target = (JTable)e.getSource();
                    int row = target.getSelectedRow();
                    int column = target.getSelectedColumn();
                    if(alive && playing && marked[row][column]!=1 && tablero[row][column]!=2 && tableroDiscovered[row][column]==0){
                    sendMovement(row, column);
                        playing=false;
                        pointsModel.setValueAt("Yo", 0, 0);
                        pointsModel.setValueAt("<html><b>Jugador 2<b/></html>", 1, 0);

                        explorar(row, column, 0);
                    }

                }
                if (e.getClickCount() == 1 && SwingUtilities.isRightMouseButton(e) ) {
                    Point p = e.getPoint();
                    int row = table.rowAtPoint( p );
                    int column= table.columnAtPoint(p);
                    if(alive && playing && marked[row][column]!=1){

                    setMarked(row, column, 1);
                    playing=false;
                    sendMarked(row,column);
                    }
                }
            }
        });
        //Interceptamos click de reinicio
        b.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                iniciar();
                reconnect=true;
                socket.disconnect();
                showMainPanel();


            }
        });
        //A??adimos zona de scroll si fuese necesario
        JScrollPane panelConScroll = new JScrollPane(table);
        add(panelConScroll);



    }
    private static void crearUI() {
        //Creamos la pantalla inicial con su t?tulo e icono
        frame = new JFrame("Buscaminas");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Configuramos el contenido
        Game panel;
        try{
            ///
            panel = new Game();
            JLayeredPane lpane = new JLayeredPane();
            frame.setPreferredSize(new Dimension(710, 540));
            frame.setLayout(new BorderLayout());
            frame.add(lpane, BorderLayout.CENTER);
            lpane.setBounds(0, 0, 710, 540);
            panel.setBounds(0, 0, 710, 540);
            panel.setOpaque(true);
            /*panel2.setBackground(new Color(43, 43, 43, 170));
            panel2.setBounds(270, 200, 300, 100);
            panel2.setBorder(new RoundedBorder(10));
            panel2.setOpaque(true);
            panel2.setLayout(new GridBagLayout());

            panel2.add(textLog);
            lpane.add(panel2, new Integer(1), 0);
            */
            lpane.add(panel, new Integer(0), 0);
            frame.pack();
            frame.setVisible(true);

        }catch(IOException e){
            e.printStackTrace();
        }





    }
    private static class RoundedBorder implements Border {

        private int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }
        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius+1, this.radius+1, this.radius+2, this.radius);
        }


        public boolean isBorderOpaque() {
            return true;
        }


        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.drawRoundRect(x,y,width-1,height-1,radius,radius);
        }
    }
    private static int random(){
        Random random = new Random();
        int rand = random.nextInt(11);
        int bomb;
        if(rand<9){
            bomb = 0;
        }else{
            bomb=1;
        }
        return bomb;
    }

    private static void rellenar(int[][] tablero){
        for(int x=0;x<tablero.length;x++){
            for(int y=0;y<tablero[x].length;y++){
                int bomb = random();
                tablero[x][y]=bomb;
                if(bomb==1){numBombs++;}
            }

            if(!drawn){
                tableModel.insertRow(x, new Object[]{});
            }


        }
    }
    private static void drawCells(){
        for(int x=0;x<tablero.length;x++){
            for(int y=0;y<tablero[x].length;y++){
                tableModel.setValueAt(new ImageIcon(Game.class.getResource("icons/block.png")), x, y);
            }
        }
    }
    private static int numNearBombs(int x, int y){
        int found=0;
        try{if (tablero[x][y+1]==1) found++;}catch(Exception e){}
        try{if (tablero[x][y-1]==1) found++;}catch(Exception e){}
        try{if (tablero[x+1][y]==1) found++;}catch(Exception e){}
        try{if (tablero[x-1][y]==1) found++;}catch(Exception e){}
        try{if (tablero[x+1][y+1]==1) found++;}catch(Exception e){}
        try{if (tablero[x-1][y-1]==1) found++;}catch(Exception e){}
        try{if (tablero[x+1][y-1]==1) found++;}catch(Exception e){}
        try{if (tablero[x-1][y+1]==1) found++;}catch(Exception e){}

        return found;
    }
    private static void setMarked(int x, int y, int who){
        if(tablero[x][y]!=2){
            tableModel.setValueAt(new ImageIcon(Game.class.getResource("icons/flag"+who+".png")), x, y);
            marked[x][y]=1;
            markedAuthor[x][y]=who;
            numMarked++;
            checkEnd();

        }
    }
    private static void explorar(int x, int y, int t){
        if(x<=9 && x>=0 && y<=9 && y>=0 && tableroDiscovered[x][y]==0){
        int numNear =  numNearBombs(x, y);
        if(tablero[x][y]==1){

            tableModel.setValueAt(new ImageIcon(Game.class.getResource("icons/mine"+(t+1)+".png")), x, y);

            if(t==0){


                tx.setText("¡Descubriste una mina! -30\n"+tx.getText());

            }
            puntos[t]=puntos[t]-30;
            updatePoints();
            tableroDiscovered[x][y]=1;
            numDiscovered++;
        }
        else if(numNear!=0){
            if(marked[x][y]==1){
                //Si alguien la habia marcado y claramente se descubre que no habia nada, se le restan puntos
                puntos[markedAuthor[x][y]-1]=puntos[markedAuthor[x][y]-1]-10;
                updatePoints();
                if(markedAuthor[x][y]-1==0){
                    tx.setText("Bandera equivocada -10\n"+tx.getText());
                }
                numMarked--;
            }
            tableModel.setValueAt(new ImageIcon(Game.class.getResource("icons/block"+numNear+".png")), x, y);
            numDiscovered++;
            tableroDiscovered[x][y]=1;
        }
        else if(tablero[x][y]==2){


        }else{
            if(marked[x][y]==1){
                //Si alguien la habia marcado y claramente se descubre que no habia nada, se le restan puntos
                puntos[markedAuthor[x][y]-1]=puntos[markedAuthor[x][y]-1]-10;
                if(markedAuthor[x][y]-1==0){
                    tx.setText("Bandera equivocada -10\n"+tx.getText());
                }
                numMarked--;

                updatePoints();
            }
            tableModel.setValueAt(new ImageIcon(Game.class.getResource("icons/discovered.png")), x, y);
            numDiscovered++;
            tablero[x][y]=2;
            tableroDiscovered[x][y]=1;

            explorar(x,y+1, t);
            explorar(x,y-1, t);
            explorar(x+1,y, t);
            explorar(x-1,y, t);

            }
            perc.setText("Descubierto: "+numDiscovered+ "%");
        }

        checkEnd();




    }
    public static void checkEnd(){
        if(numDiscovered+numMarked==100){
            recuento();
            if(puntos[0]>puntos[1]){
                JOptionPane.showMessageDialog(frame, "¡Ganaste!", "Juego", JOptionPane.INFORMATION_MESSAGE);
            }else{
                JOptionPane.showMessageDialog(frame, "¡Perdiste!", "Juego", JOptionPane.INFORMATION_MESSAGE);
            }
            alive=false;
        }
    }
    public static void updatePoints(){
        pointsModel.setValueAt(puntos[0], 0, 1);
        pointsModel.setValueAt(puntos[1], 1, 1);

    }
    public static void recuento(){
        for(int x=0;x<10;x++){
            for(int y=0;y<10;y++){
                if(marked[x][y]==1 && tablero[x][y]==1){
                    tableModel.setValueAt(new ImageIcon(Game.class.getResource("icons/flag"+markedAuthor[x][y]+"mine.png")), x, y);
                    puntos[markedAuthor[x][y]-1]=puntos[markedAuthor[x][y]-1]+20;
                    if(markedAuthor[x][y]-1==0)
                    {
                        tx.setText("¡Bandera correcta! +20\n"+tx.getText());

                    }
                    updatePoints();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(marked[x][y]==1 && tablero[x][y]!=1){
                    tableModel.setValueAt(new ImageIcon(Game.class.getResource("icons/discovered.png")), x, y);
                    puntos[markedAuthor[x][y]-1]=puntos[markedAuthor[x][y]-1]-10;
                    if(markedAuthor[x][y]-1==0)
                    {
                        tx.setText("¡Bandera vacia! -10\n"+tx.getText());

                    }
                    updatePoints();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public static void iniciar(){
        // Generamos el tablero de 5x5
        tablero = new int[10][10];
        // Generamos la interfaz gr?fica
        rellenar(tablero);
        drawCells();
        drawn=true;
        alive=true;
        numDiscovered=0;
        marked = new int[10][10];
        markedAuthor = new int[10][10];
        puntos[0]=0;
        puntos[1]=0;
        updatePoints();
        playing=false;
        tableroDiscovered=new int[10][10];
        tx.setText("");
        //We instanciate the Timer Timer timer = new Timer();

        //And then we launch the fade-out, waiting 500ms before starting
        //Then we gradually fade out every 5ms

    }
    public static void showMainPanel(){
        optionPane = new JOptionPane("Conectando al servidor de juegos...", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);

        dialog = new JDialog();
        dialog.setTitle("Buscaminas");
        dialog.setModal(true);

        dialog.setContentPane(optionPane);

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.pack();

        dialog.setVisible(true);
    }
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                drawn=false;
                crearUI();
                iniciar();
                startSocket();
                showMainPanel();


            }
        });
    }
    public static void shareTablero() {

        try {
            socket.emit("shareTablero", new JSONObject().put("tablero", tablero));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static void sendMovement(int x, int y){
        try {
            socket.emit("sendMovement", new JSONObject().put("partner", partner).put("x", x).put("y", y));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void sendMarked(int x, int y){
        try {
            socket.emit("sendMarked", new JSONObject().put("partner", partner).put("x", x).put("y", y));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void startSocket(){

         socket = new IOSocket("http://localhost:8081", new MessageCallback() {
            @Override

            public void on(String event, JSONObject... data) {
                // Handle events

                JSONObject a = data[0];
                String action = (String) a.get("action");
                if(action.equals("found")){
                    dialog.setVisible(false);
                    JSONObject info = (JSONObject) a.get("info");
                    partner =  (String)info.get("partner");
                    if(info.get("self").equals(info.get("turn"))){
                        playing=true;
                        pointsModel.setValueAt("<html><b>Yo<b/></html>", 0, 0);
                        shareTablero();

                    }
                }

                if(action.equals("sendTablero")){
                    dialog.setVisible(false);
                    partner =  (String)a.get("partner");
                    JSONArray r = (JSONArray) a.get("tablero");
                    for(int i=0;i<r.length();i++){
                        JSONArray n = r.getJSONArray(i);
                        for(int x=0;x<n.length();x++){
                            tablero[i][x]=n.optInt(x);
                        }
                    }

                }
                if(action.equals("doMovement")){
                    int x = a.getInt("x");
                    int y = a.getInt("y");
                    explorar(x, y, 1);
                    pointsModel.setValueAt("<html><b>Yo<b/></html>", 0, 0);
                    pointsModel.setValueAt("Jugador 2", 1, 0);
                    playing=true;


                }

                if(action.equals("doMark")){
                    int x = a.getInt("x");
                    int y = a.getInt("y");
                    setMarked(x, y, 2);
                    pointsModel.setValueAt("<html><b>Yo<b/></html>", 0, 0);
                    pointsModel.setValueAt("Jugador 2", 1, 0);
                    playing=true;

                }
                if(action.equals("partnerEnd")){
                    optionPane.setMessage("El otro jugador abandonó la partida.");
                    JButton button = new JButton();
                    button.setText("Cerrar");
                    button.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            super.mouseClicked(e);
                            dialog.setVisible(false);
                        }
                    });
                    dialog.setMinimumSize(new Dimension(450, 150));
                    optionPane.add(button);
                    dialog.setVisible(true);

                }


            }

            @Override
            public void onMessage(String message) {
                // Handle simple messages
            }

            @Override
            public void onMessage(JSONObject message) {

            }

            @Override
            public void onConnect() {
                // Socket connection opened
                System.out.print("Connected to server!");
                optionPane.setMessage("Buscando compañero online...");
            }

            @Override
            public void onDisconnect() {
                // Socket connection closed
                if(reconnect){
                    try {
                        socket.connect();

                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        try {
            socket.connect();
        } catch (IOException e) {
            System.out.print("Error server");
            //optionPane.setMessage("El servidor de juegos está inactivo o no es posible encontrarlo.");
        }
    }
}
