export type CardId = number
export type PlayerId = string

export interface Card {
    name: string
    set: string
    numberInSet: number
}

export type ZoneEntry = [CardId, Card]
export type Zone = [ZoneEntry?]