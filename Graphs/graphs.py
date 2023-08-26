import matplotlib.pyplot as plt


def plot_response_times(file_path):
    try:
        with open(file_path, 'r') as file:
            response_times = [float(line.strip()) for line in file.readlines()]

            # Creazione dell'asse degli indici
            indices = [(i + 1) * 1024 * 7 for i in range(len(response_times))]

            # Creazione del grafico
            plt.figure(figsize=(10, 6))
            plt.plot(indices, response_times, marker='o')
            plt.title('Tempo di Risposta')
            plt.xlabel('Numero di Utenti')
            plt.ylabel('Tempo di Risposta')
            plt.grid(True)
            plt.show()

    except FileNotFoundError:
        print("File non trovato.")
    except Exception as e:
        print(f"Si è verificato un errore: {e}")


if __name__ == "__main__":
    file_path = "../batch_reports/response_times_first_perquisition.dat"  # Inserisci il percorso corretto del tuo file .dat
    plot_response_times(file_path)
