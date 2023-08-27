import matplotlib.pyplot as plt


def plot(file_path, statistic, node):
    try:
        with open(file_path, 'r') as file:
            response_times = [float(line.strip()) for line in file.readlines()]

            # Creazione dell'asse degli indici
            indices = [(i + 1) * 1024 * 7 for i in range(len(response_times))]

            # Creazione del grafico
            plt.figure(figsize=(10, 6))
            plt.plot(indices, response_times, marker='o')
            plt.title(f"{statistic} - {node}")
            plt.xlabel('Numero di Utenti')
            plt.ylabel(statistic)
            plt.grid(True)
            plt.show()

    except FileNotFoundError:
        print("File non trovato.")
    except Exception as e:
        print(f"Si è verificato un errore: {e}")


if __name__ == "__main__":

    response_time = "Tempo di Risposta"
    utilization = "Utilizzazione"
    ticket_check = "Controllo biglietti"
    first_perquisition = "Prima perquisizione"
    turnstiles = "Tornelli"
    second_perquisition = "Seconda perquisizione"

    plot("../batch_reports/response_times_ticket_check.dat", response_time, ticket_check)
    plot("../batch_reports/response_times_first_perquisition.dat", response_time, first_perquisition)
    plot("../batch_reports/response_times_turnstiles.dat", response_time, turnstiles)
    plot("../batch_reports/response_times_second_perquisition.dat", response_time, second_perquisition)
    plot("../batch_reports/utilizations_ticket_check.dat", utilization, ticket_check)
    plot("../batch_reports/utilizations_first_perquisition.dat", utilization, first_perquisition)
    plot("../batch_reports/utilizations_turnstiles.dat", utilization, turnstiles)
    plot("../batch_reports/utilizations_second_perquisition.dat", utilization, second_perquisition)
