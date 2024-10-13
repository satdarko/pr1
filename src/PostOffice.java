import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class PostOffice {
    private static final int SENDER_COUNT = 3; // Кількість відправників
    private static final int WORKING_HOURS = 8; // Час роботи пошти в секундах
    private static final Semaphore senderSemaphore = new Semaphore(1); // Семафор для контролю доступу до працівника пошти
    private static boolean isPostOfficeOpen = true; // Стан пошти
    private static int processedPackages = 0; // Кількість оброблених посилок
    private static int finishedSenders = 0; // Кількість відправників, які завершили відправлення
    private static int[] senderIds = new int[SENDER_COUNT]; // Ідентифікатори відправників

    public static void main(String[] args) throws InterruptedException {
        // Створення потоку для працівника пошти
        Thread workerThread = new Thread(new PostalWorker());
        workerThread.start();

        // Створення кількох відправників
        for (int i = 1; i <= SENDER_COUNT; i++) {
            new Thread(new Sender(i)).start();
        }

        // Симуляція робочих годин пошти
        TimeUnit.SECONDS.sleep(WORKING_HOURS);
        closePostOffice();

        // Чекаємо, поки працівник завершить обробку всіх посилок
        workerThread.join();
        System.out.println("Програма закінчила свою роботу.");
    }

    // Метод для закриття пошти після робочих годин
    public static void closePostOffice() {
        System.out.println("Пошта закривається. Більше немає доступу до прийому листів.");
        isPostOfficeOpen = false;

    }

    // Клас PostalWorker реалізує Runnable для симуляції роботи працівника пошти
    static class PostalWorker implements Runnable {
        @Override
        public void run() {
            while (processedPackages < finishedSenders || (SENDER_COUNT > finishedSenders && isPostOfficeOpen)) {
                try {
                    // Затримка для обробки
                    TimeUnit.MILLISECONDS.sleep(1000);
                    synchronized (PostOffice.class) {
                        // Якщо ще є посилки для обробки
                        if (processedPackages < finishedSenders) {
                            int currentSenderId = senderIds[processedPackages]; // Отримуємо ID відправника
                            System.out.println("Працівник пошти обробляє лист/посилку від відправника " + currentSenderId + ".");
                            processedPackages++; // Збільшуємо кількість оброблених посилок
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("Працівник пошти був перерваний.");
                }
            }
            System.out.println("Працівник пошти закінчив свою роботу і йде додому.");
        }
    }

    // Клас Sender реалізує Runnable для створення відправників
    static class Sender implements Runnable {
        private int senderId;
        private boolean sessionActive = true; // Відстеження активності сесії

        public Sender(int senderId) {
            this.senderId = senderId;
        }

        @Override
        public void run() {
            try {
                while (sessionActive) {
                    if (senderSemaphore.tryAcquire(1, TimeUnit.SECONDS)) {
                        synchronized (PostOffice.class) { // Синхронізація для правильного порядку повідомлень
                            if (!isPostOfficeOpen) {
                                System.out.println("Відправник " + senderId + " виявляє, що пошта закрита і йде.");
                                sessionActive = false; // Завершення сесії
                                senderSemaphore.release(); // Випуск семафору
                                break;
                            }
                            System.out.println("Відправник " + senderId + " відправляє лист/посилку.");
                        }

                        // Симуляція відправки листа/посилки
                        TimeUnit.MILLISECONDS.sleep(1500); // Затримка на відправку

                        synchronized (PostOffice.class) { // Гарантуємо правильну послідовність
                            if (isPostOfficeOpen) {
                                System.out.println("Відправник " + senderId + " завершив відправлення.");
                                senderIds[finishedSenders] = senderId; // Зберігаємо ID відправника
                                finishedSenders++; // Збільшуємо кількість завершених відправлень
                                senderSemaphore.release(); // Випуск семафору для наступного відправника
                            }
                        }
                        sessionActive = false; // Відправник завершує успішно
                    } else if (!isPostOfficeOpen) {
                        // Пошта закрита, відправник не може відправити
                        synchronized (PostOffice.class) {
                            System.out.println("Відправник " + senderId + " виявляє, що пошта закрита і йде.");
                        }
                        sessionActive = false; // Завершення сесії
                        break;
                    } else {
                        // Пошта відкрита, але працівник зайнятий
                        System.out.println("Відправник " + senderId + " чекає на працівника пошти.");
                        TimeUnit.MILLISECONDS.sleep(500); // Невелика затримка перед повторною спробою
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Відправник " + senderId + " був перерваний.");
            }
        }
    }
}