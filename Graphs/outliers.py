import numpy as np

def find_outliers_IQR(data, threshold=1.5):
    # Calcola il primo e il terzo quartile
    Q1, Q3 = np.percentile(data, [25, 75])
    # Calcola l'IQR
    IQR = Q3 - Q1
    # Calcola la soglia inferiore e superiore
    lower_bound = Q1 - (threshold * IQR)
    upper_bound = Q3 + (threshold * IQR)
    return lower_bound, upper_bound

    # Identifica gli outlier
    # outliers = [x for x in data if x < lower_bound or x > upper_bound]
    # return outliers


if __name__ == "__main__":

    data_list = []

    try:
        with open("../replication_reports/queue_populations_first_perquisition.dat", 'r') as file:
            for line in file:
                value = float(line.strip())
                data_list.append(value)

    except FileNotFoundError:
        print("File not found.")
    except ValueError:
        print("Error in coversion to float.")
    except Exception as e:
        print(f"An error occurred: {e}")

    print(data_list)
    print(max(data_list))
    print(find_outliers_IQR(data_list))
